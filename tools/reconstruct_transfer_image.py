#!/usr/bin/env python3
"""Reconstruct a block-OTA partition from transfer.list + brotli new.dat.

Transfer-list header line 2 is the number of blocks written by write commands,
not the image size or the highest referenced block index. The output image is
sized from all referenced ranges and then expanded to the ext4 superblock size.
"""
import os
import sys
import struct
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

version, total_write_blocks, stash_entries, max_stashed = map(int, lines[:4])
if version != 4:
    fail(f"unsupported transfer-list version: {version}")

commands = []
max_range_end = 0
expected_new_blocks = 0
zero_blocks = 0
erase_blocks = 0
for line_no, line in enumerate(lines[4:], start=5):
    command, _, argtext = line.partition(" ")
    if command not in ("new", "zero", "erase"):
        fail(f"unsupported command on line {line_no}: {command}")
    try:
        fields = [int(x) for x in argtext.split(",") if x]
        range_count = fields[0]
        ranges = fields[1:]
    except Exception:
        fail(f"invalid range command on line {line_no}: {line}")
    if range_count != len(ranges) or range_count % 2:
        fail(f"invalid range count on line {line_no}: {line}")
    pairs = list(zip(ranges[0::2], ranges[1::2]))
    command_blocks = 0
    for start, end in pairs:
        if start < 0 or end < start:
            fail(f"invalid block range on line {line_no}: {start}-{end}")
        command_blocks += end - start
        max_range_end = max(max_range_end, end)
    if command == "new":
        expected_new_blocks += command_blocks
    elif command == "zero":
        zero_blocks += command_blocks
    else:
        erase_blocks += command_blocks
    commands.append((command, pairs))

image_blocks = max_range_end
header_matches_write_count = total_write_blocks == expected_new_blocks + zero_blocks

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
actual_new_blocks = 0
with open(output_path, "wb") as out, open(new_path, "rb") as newdata:
    out.truncate(image_blocks * BLOCK)
    for command, pairs in commands:
        if command != "new":
            # An image reconstructed from scratch starts with zeroed blocks.
            continue
        for start, end in pairs:
            remaining = (end - start) * BLOCK
            out.seek(start * BLOCK)
            while remaining:
                chunk = newdata.read(min(4 * 1024 * 1024, remaining))
                if not chunk:
                    fail("new.dat ended before transfer-list data was consumed")
                out.write(chunk)
                remaining -= len(chunk)
            actual_new_blocks += end - start
    if newdata.read(1):
        fail("unused bytes remain in new.dat after transfer-list commands")

os.remove(new_path)
with open(output_path, "rb") as f:
    f.seek(1024)
    sb = f.read(1024)
ext4_magic = len(sb) >= 58 and sb[56:58] == b"\x53\xef"
fs_blocks = 0
fs_block_size = 0
if ext4_magic:
    blocks_lo = struct.unpack_from("<I", sb, 4)[0]
    log_block_size = struct.unpack_from("<I", sb, 24)[0]
    fs_block_size = 1024 << log_block_size
    blocks_hi = struct.unpack_from("<I", sb, 0x150)[0] if len(sb) >= 0x154 else 0
    fs_blocks = (blocks_hi << 32) | blocks_lo
    fs_bytes = fs_blocks * fs_block_size
    if fs_bytes > os.path.getsize(output_path):
        with open(output_path, "ab") as out:
            out.truncate(fs_bytes)

image_bytes = os.path.getsize(output_path)
print(f"version={version} header_total_write_blocks={total_write_blocks}")
print(f"new_blocks={actual_new_blocks} zero_blocks={zero_blocks} erase_blocks={erase_blocks}")
print(f"write_count_matches_new_plus_zero={header_matches_write_count}")
print(f"max_referenced_block_end={max_range_end} image_bytes={image_bytes}")
print(f"new_dat_bytes={len(raw)} ext4_superblock_magic_at_1080={ext4_magic} ext4_blocks={fs_blocks} ext4_block_size={fs_block_size}")
if fs_blocks:
    fs_bytes = fs_blocks * fs_block_size
    written_blocks = actual_new_blocks + zero_blocks
    touched_blocks = written_blocks + erase_blocks
    write_coverage = 100.0 * written_blocks / fs_blocks
    touched_coverage = 100.0 * touched_blocks / fs_blocks
    print(f"filesystem_declared_bytes={fs_bytes}")
    print(f"new_plus_zero_block_coverage={written_blocks}/{fs_blocks} ({write_coverage:.2f}%)")
    print(f"new_zero_plus_erase_coverage={touched_blocks}/{fs_blocks} ({touched_coverage:.2f}%)")
    print(f"transfer_covers_full_filesystem_by_operations={touched_blocks >= fs_blocks}")
    if touched_blocks < fs_blocks:
        print("WARNING: transfer list leaves declared filesystem blocks untouched. A zero-based reconstruction is incomplete; it needs a compatible base image or a verified full-image source.")
if not header_matches_write_count:
    print("WARNING: transfer-list header does not equal the number of blocks covered by new + zero commands.")
