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


def transfer_summary(data: bytes) -> list[str]:
    """Validate range-based transfer operations and count operation-specific commands."""
    lines = data.decode("utf-8", "replace").splitlines()
    out = ["header=" + " | ".join(lines[:4])]
    if len(lines) < 4:
        return out + ["ERROR: transfer list has fewer than four header lines"]
    counts: dict[str, int] = {}
    ranges_by_cmd: dict[str, int] = {}
    malformed_ranges = 0
    unparsed_ops: dict[str, int] = {}
    for line in lines[4:]:
        cmd, sep, arg = line.partition(" ")
        counts[cmd] = counts.get(cmd, 0) + 1
        if cmd not in ("new", "zero", "erase"):
            # move/bsdiff/imgdiff/stash/free have different grammars; don't
            # falsely label them malformed when checking simple range commands.
            unparsed_ops[cmd] = unparsed_ops.get(cmd, 0) + 1
            continue
        if not sep:
            malformed_ranges += 1
            continue
        try:
            vals = [int(x) for x in arg.split(",")]
            if not vals or vals[0] != len(vals) - 1 or (len(vals) - 1) % 2:
                malformed_ranges += 1
                continue
            ranges = list(zip(vals[1::2], vals[2::2]))
            if any(start < 0 or finish < start for start, finish in ranges):
                malformed_ranges += 1
                continue
            ranges_by_cmd[cmd] = ranges_by_cmd.get(cmd, 0) + sum(
                finish - start for start, finish in ranges
            )
        except ValueError:
            malformed_ranges += 1
    out.extend((
        f"command_counts={counts}",
        f"range_blocks_by_command={ranges_by_cmd}",
        f"malformed_range_commands={malformed_ranges}",
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
