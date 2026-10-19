#!/usr/bin/env python3
"""Compare ROM ZIP installer scripts and block-OTA metadata without modifying archives."""
from __future__ import annotations

import difflib
import hashlib
import re
import zipfile
from pathlib import Path

ARCHIVES = {
    "ORIGINAL LINEAGEOS": Path("work/original/rom.zip"),
    "LUCID OS": Path("work/lucid/rom.zip"),
}
TEXT_SUFFIXES = (
    "updater-script",
    "dynamic_partitions_op_list",
    "META-INF/com/android/metadata",
    "payload_properties.txt",
)
BINARY_SUFFIXES = (
    "boot.img", "dtbo.img", "vbmeta.img", "super_dummy.img",
    "system.new.dat.br", "vendor.new.dat.br", "product.new.dat.br",
    "system_ext.new.dat.br", "odm.new.dat.br",
)
INSTALLER_RE = re.compile(
    r"format|wipe|delete|block_image_update|block_image_verify|package_extract|"
    r"super_dummy|dynamic_partitions|product|vendor|system|cust|metadata", re.I
)


def digest_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def digest_file(path: Path) -> str:
    """Hash large ROM archives in bounded memory rather than reading a 1+ GB ZIP at once."""
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(8 * 1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def digest_zip_entry(z: zipfile.ZipFile, name: str) -> str:
    """Hash a potentially large ZIP member in bounded memory."""
    h = hashlib.sha256()
    with z.open(name) as stream:
        for chunk in iter(lambda: stream.read(8 * 1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def parse_ranges(text: str) -> tuple[list[tuple[int, int]] | None, str | None]:
    """Parse Android RangeSet syntax: count,start,end[,start,end...]."""
    try:
        values = [int(value) for value in text.split(",")]
    except ValueError:
        return None, "non-integer range field"
    if not values or values[0] != len(values) - 1 or (len(values) - 1) % 2:
        return None, "range count does not match fields"
    ranges = list(zip(values[1::2], values[2::2]))
    if any(start < 0 or finish < start for start, finish in ranges):
        return None, "negative or reversed range"
    return ranges, None


def transfer_summary(data: bytes) -> list[str]:
    """Validate transfer-list range commands and reconcile target writes with header."""
    lines = data.decode("utf-8", "replace").splitlines()
    out = ["header=" + " | ".join(lines[:4])]
    if len(lines) < 4:
        return out + ["ERROR: transfer list has fewer than four header lines"]
    try:
        expected_write_blocks = int(lines[1])
        if expected_write_blocks < 0:
            raise ValueError
    except ValueError:
        return out + ["ERROR: transfer-list write-block header is not a nonnegative integer"]

    counts: dict[str, int] = {}
    range_blocks_by_cmd: dict[str, int] = {}
    target_write_blocks_by_cmd: dict[str, int] = {}
    malformed_ranges = 0
    malformed_target_ranges = 0
    unparsed_ops: dict[str, int] = {}

    for line in lines[4:]:
        cmd, sep, arg = line.partition(" ")
        counts[cmd] = counts.get(cmd, 0) + 1
        if cmd in ("new", "zero", "erase"):
            ranges, error = parse_ranges(arg) if sep else (None, "missing range argument")
            if error or ranges is None:
                malformed_ranges += 1
                continue
            block_count = sum(finish - start for start, finish in ranges)
            range_blocks_by_cmd[cmd] = range_blocks_by_cmd.get(cmd, 0) + block_count
            if cmd in ("new", "zero"):
                target_write_blocks_by_cmd[cmd] = target_write_blocks_by_cmd.get(cmd, 0) + block_count
            continue

        # Android block_image_update target ranges: move <hash> <tgt_range> ...;
        # bsdiff/imgdiff <offset> <len> <src_hash> <tgt_hash> <tgt_range> ...
        tokens = line.split()
        target_index = {"move": 2, "bsdiff": 5, "imgdiff": 5}.get(cmd)
        if target_index is not None:
            if len(tokens) <= target_index:
                malformed_target_ranges += 1
                continue
            ranges, error = parse_ranges(tokens[target_index])
            if error or ranges is None:
                malformed_target_ranges += 1
                continue
            block_count = sum(finish - start for start, finish in ranges)
            target_write_blocks_by_cmd[cmd] = target_write_blocks_by_cmd.get(cmd, 0) + block_count
        else:
            # stash/free/abort and other commands don't have the same target-range grammar.
            unparsed_ops[cmd] = unparsed_ops.get(cmd, 0) + 1

    counted_write_blocks = sum(target_write_blocks_by_cmd.values())
    out.extend((
        f"command_counts={counts}",
        f"range_blocks_by_command={range_blocks_by_cmd}",
        f"target_write_blocks_by_command={target_write_blocks_by_cmd}",
        f"malformed_range_commands={malformed_ranges}",
        f"malformed_target_range_commands={malformed_target_ranges}",
        f"expected_write_blocks={expected_write_blocks}",
        f"counted_target_write_blocks={counted_write_blocks}",
        f"write_block_count_matches_header={counted_write_blocks == expected_write_blocks}",
        f"operation_specific_commands_not_interpreted={unparsed_ops}",
    ))
    return out

def read_entry(z: zipfile.ZipFile, names: list[str], suffix: str) -> tuple[str, bytes] | None:
    matches = [name for name in names if name.endswith(suffix)]
    if len(matches) != 1:
        print(f"WARNING: expected exactly one {suffix}, found {len(matches)}: {matches}")
        return None
    return matches[0], z.read(matches[0])


def main() -> int:
    loaded: dict[str, tuple[zipfile.ZipFile, list[str]]] = {}
    failed = False
    try:
        for label, path in ARCHIVES.items():
            print("\n" + "=" * 88 + "\n" + label + "\n" + "=" * 88)
            if not path.is_file():
                print(f"ERROR: missing archive {path}")
                failed = True
                continue
            z = zipfile.ZipFile(path)
            loaded[label] = (z, z.namelist())
            bad = z.testzip()
            print(f"archive_bytes={path.stat().st_size} archive_sha256={digest_file(path)}")
            print(f"zip_test={'PASS' if bad is None else 'FAIL:' + bad}")
            if bad is not None:
                failed = True
            duplicate_names = sorted({name for name in z.namelist() if z.namelist().count(name) > 1})
            if duplicate_names:
                print("ERROR: duplicate ZIP entry names=" + repr(duplicate_names))
                failed = True
            print(f"entry_count={len(z.namelist())}")
            for info in sorted(z.infolist(), key=lambda x: x.filename):
                if any(key in info.filename.lower() for key in
                       ("product", "system.transfer.list", "vendor.transfer.list",
                        "odm.transfer.list", "system_ext.transfer.list",
                        "dynamic_partitions", "updater-script", "metadata", "super_dummy")):
                    print(f"{info.file_size:>12} {info.filename}")
        if len(loaded) != 2:
            return 1

        (orig_z, orig_names) = loaded["ORIGINAL LINEAGEOS"]
        (lucid_z, lucid_names) = loaded["LUCID OS"]
        orig_set, lucid_set = set(orig_names), set(lucid_names)
        print("\n" + "=" * 88 + "\nARCHIVE ENTRY DIFFERENCES\n" + "=" * 88)
        print("only_in_original=" + repr(sorted(orig_set - lucid_set)))
        print("only_in_lucid=" + repr(sorted(lucid_set - orig_set)))
        print(f"common_entries={len(orig_set & lucid_set)}")

        important = sorted({
            name for name in orig_set | lucid_set
            if name.endswith(TEXT_SUFFIXES) or name.endswith(".transfer.list")
        })
        for name in important:
            print(f"\n--- ENTRY {name} ---")
            if name not in orig_set:
                print("ORIGINAL: MISSING")
                print("LUCID SHA256: " + digest_bytes(lucid_z.read(name)))
                continue
            if name not in lucid_set:
                print("LUCID: MISSING")
                print("ORIGINAL SHA256: " + digest_bytes(orig_z.read(name)))
                continue
            a, b = orig_z.read(name), lucid_z.read(name)
            print(f"ORIGINAL size={len(a)} sha256={digest_bytes(a)}")
            print(f"LUCID    size={len(b)} sha256={digest_bytes(b)}")
            if a == b:
                print("content=IDENTICAL")
                continue
            print("content=DIFFERENT")
            if name.endswith(".transfer.list"):
                print("ORIGINAL transfer summary:")
                print("\n".join(transfer_summary(a)))
                print("LUCID transfer summary:")
                print("\n".join(transfer_summary(b)))
            if name.endswith(TEXT_SUFFIXES):
                diff = difflib.unified_diff(
                    a.decode("utf-8", "replace").splitlines(),
                    b.decode("utf-8", "replace").splitlines(),
                    fromfile="ORIGINAL/" + name,
                    tofile="LUCID/" + name,
                    lineterm="",
                )
                for line in diff:
                    print(line)

        print("\n" + "=" * 88 + "\nCRITICAL IMAGE AND PAYLOAD HASHES\n" + "=" * 88)
        binary_names = sorted({
            name for _, names in loaded.values() for name in names
            if name.endswith(BINARY_SUFFIXES)
        })
        for name in binary_names:
            print(f"\n--- BINARY {name} ---")
            for label, (z, names) in loaded.items():
                if name not in names:
                    print(f"{label}: MISSING")
                    continue
                info = z.getinfo(name)
                print(f"{label}: size={info.file_size} sha256={digest_zip_entry(z, name)}")

        for label, (z, names) in loaded.items():
            for suffix in ("product.transfer.list", "product.new.dat.br"):
                matches = [name for name in names if name.endswith(suffix)]
                if len(matches) != 1:
                    print(f"WARNING [{label}]: expected exactly one {suffix}, found {len(matches)}")
    finally:
        for z, _ in loaded.values():
            z.close()
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
