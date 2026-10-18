#!/usr/bin/env python3
"""Reconstruct a full block OTA partition from transfer.list + brotli-compressed new.dat.

Supports the full-OTA transfer-list commands used by the current LucidOS archive:
new, erase and zero. It deliberately fails on patch-dependent commands rather
than silently producing a corrupted image.
"""
import os
import sys
import brotli

BLOCK = 4096

def fail(msg):
    raise SystemExit(msg)

if len(sys.argv) != 4:
    fail("usage: reconstruct_transfer_image.py TRANSFER_LIST NEW_DAT_BR OUTPUT_IMG")

transfer_path, compressed_path, output_path = sys.argv[1:]
with open(transfer_path, "r", encoding="utf-8") as f:
    lines = [line.strip() for line in f if line.strip()]

if len(lines) < 4:
    fail("transfer list is truncated")
version, total_blocks, stash_entries, max_stashed = map(int, lines[:4])
if version != 4:
    fail(f"unsupported transfer-list version: {version}")

with open(compressed_path, "rb") as f:
    compressed = f.read()
try:
    raw = brotli.decompress(compressed)
except Exception as exc:
    fail(f"brotli decompression failed: {exc}")

new_path = output_path + ".newdata"
with open(new_path, "wb") as f:
    f.write(raw)

os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
with open(output_path, "wb") as out, open(new_path, "rb") as newdata:
    out.truncate(total_blocks * BLOCK)
    new_blocks = 0
    skipped = 0
    for line_no, line in enumerate(lines[4:], start=5):
        command, _, argtext = line.partition(" ")
        if command in ("erase", "zero"):
            skipped += 1
            continue
        if command != "new":
            fail(f"unsupported command on line {line_no}: {command}")
        try:
            fields = [int(x) for x in argtext.split(",") if x]
            range_count = fields[0]
            ranges = fields[1:]
        except Exception:
            fail(f"invalid new command on line {line_no}: {line}")
        if range_count != len(ranges) or range_count % 2:
            fail(f"invalid range count on line {line_no}: {line}")
        for start, end in zip(ranges[0::2], ranges[1::2]):
            if start < 0 or end < start or end > total_blocks:
                fail(f"invalid block range on line {line_no}: {start}-{end}")
            remaining = (end - start) * BLOCK
            out.seek(start * BLOCK)
            while remaining:
                chunk = newdata.read(min(4 * 1024 * 1024, remaining))
                if not chunk:
                    fail("new.dat ended before transfer-list data was consumed")
                out.write(chunk)
                remaining -= len(chunk)
            new_blocks += end - start
    if newdata.read(1):
        fail("unused bytes remain in new.dat after transfer-list commands")

os.remove(new_path)
with open(output_path, "rb") as f:
    f.seek(1024 + 56)
    ext4_magic = f.read(2) == b"\x53\xef"
print(f"version={version} total_blocks={total_blocks} image_bytes={total_blocks * BLOCK}")
print(f"new_blocks={new_blocks} erase_or_zero_commands={skipped}")
print(f"new_dat_bytes={len(raw)} ext4_superblock_magic_at_1080={ext4_magic}")
