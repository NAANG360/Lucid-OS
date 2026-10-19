#!/usr/bin/env python3
"""Compare ROM ZIP installer and block-OTA metadata without modifying either archive."""
from __future__ import annotations
import hashlib
import re
import zipfile
from pathlib import Path

ARCHIVES = {
    "ORIGINAL LINEAGEOS": Path("work/original/rom.zip"),
    "LUCID OS": Path("work/lucid/rom.zip"),
}
INTERESTING = (
    "updater-script", "dynamic_partitions_op_list", "product.transfer.list",
    "META-INF/com/android/metadata", "payload_properties.txt",
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
    lines = data.decode("utf-8", "replace").splitlines()
    out = ["header=" + " | ".join(lines[:4])]
    if len(lines) < 4:
        return out + ["ERROR: transfer list has fewer than four header lines"]
    counts: dict[str, int] = {}
    ranges_by_cmd: dict[str, int] = {}
    malformed = 0
    for line in lines[4:]:
        cmd, sep, arg = line.partition(" ")
        counts[cmd] = counts.get(cmd, 0) + 1
        if not sep:
            malformed += 1
            continue
        try:
            vals = [int(x) for x in arg.split(",")]
            if not vals or vals[0] != len(vals) - 1 or (len(vals) - 1) % 2:
                malformed += 1
                continue
            for start, end in zip(vals[1::2], vals[2::2]):
                if start < 0 or end < start:
                    malformed += 1
                    continue
                ranges_by_cmd[cmd] = ranges_by_cmd.get(cmd, 0) + end - start
        except ValueError:
            malformed += 1
    out.append(f"command_counts={counts}")
    out.append(f"block_counts_by_command={ranges_by_cmd}")
    out.append(f"malformed_commands={malformed}")
    return out

def main() -> int:
    failed = False
    for label, path in ARCHIVES.items():
        print("\n" + "=" * 88 + "\n" + label + "\n" + "=" * 88)
        if not path.is_file():
            print(f"ERROR: missing archive {path}")
            failed = True
            continue
        with zipfile.ZipFile(path) as z:
            bad = z.testzip()
            print(f"archive_bytes={path.stat().st_size} archive_sha256={digest_file(path)}")
            print(f"zip_test={'PASS' if bad is None else 'FAIL:' + bad}")
            names = z.namelist()
            print("entry_count=" + str(len(names)))
            for suffix in INTERESTING:
                matches = [n for n in names if n.endswith(suffix)]
                print(f"\n[{suffix}] entries={matches}")
                for name in matches:
                    data = z.read(name)
                    print(f"size={len(data)} sha256={digest_bytes(data)}")
                    if suffix.endswith("updater-script"):
                        for i, line in enumerate(data.decode("utf-8", "replace").splitlines(), 1):
                            if INSTALLER_RE.search(line):
                                print(f"{i}: {line}")
                    elif suffix == "product.transfer.list":
                        print("\n".join(transfer_summary(data)))
                    else:
                        print(data.decode("utf-8", "replace")[:30000])
            print("\n[partition/installer archive entries]")
            for name in names:
                if any(k in name.lower() for k in ("product", "dynamic_partitions", "updater-script", "metadata", "super_dummy")):
                    print(f"{z.getinfo(name).file_size:>12} {name}")
            for suffix in ("product.transfer.list", "product.new.dat.br"):
                matches = [n for n in names if n.endswith(suffix)]
                if len(matches) != 1:
                    print(f"WARNING: expected exactly one {suffix}, found {len(matches)}")
    return 1 if failed else 0

if __name__ == "__main__":
    raise SystemExit(main())
