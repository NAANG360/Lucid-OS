#!/usr/bin/env python3
"""Safely inventory public Android firmware bundles without executing or extracting them.

The tool records archive members, hashes regular files, detects common Android/Qualcomm
image signatures, and flags whether a file could plausibly be a full raw eMMC image.
It deliberately does not execute firehose programmers or modify devices.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import struct
import tarfile
import zipfile
from pathlib import Path
from typing import BinaryIO


def sha256_stream(stream: BinaryIO) -> tuple[str, int]:
    digest = hashlib.sha256()
    size = 0
    while True:
        block = stream.read(1024 * 1024)
        if not block:
            break
        size += len(block)
        digest.update(block)
    return digest.hexdigest(), size


def classify_header(header: bytes, size: int) -> list[str]:
    labels: list[str] = []
    if header.startswith(b"ANDROID!"):
        labels.append("android-boot-image")
    if header.startswith(b"AVB0"):
        labels.append("android-vbmeta")
    if header.startswith(b"CrAU"):
        labels.append("android-ota-payload")
    if header.startswith(b"\x3a\xff\x26\xed"):
        labels.append("android-sparse-image")
    if header.startswith(b"QFI\xfb"):
        labels.append("qcow2-image")
    if header.startswith(b"\x7fELF"):
        labels.append("elf-binary-possibly-firmware")
    if len(header) >= 512 and header[510:512] == b"\x55\xaa":
        labels.append("mbr-signature-at-offset-510")
    # GPT's EFI PART header lives at LBA 1, byte offset 512, not byte zero.
    if size >= 1024:
        labels.append("gpt-check-required-at-offset-512")
    if size >= 50_000_000_000:
        labels.append("size-compatible-with-58-24-GiB-lavender-user-area")
    elif size < 1_000_000_000:
        labels.append("not-a-full-58-24-GiB-user-area-by-size-alone")
    return labels or ["unknown-or-raw-data"]


def inspect_file(path: Path) -> dict:
    with path.open("rb") as f:
        head = f.read(4096)
        f.seek(0)
        digest, size = sha256_stream(f)
    result = {
        "path": str(path),
        "size_bytes": size,
        "sha256": digest,
        "classifications": classify_header(head, size),
    }
    if len(head) >= 1024 and head[512:520] == b"EFI PART":
        result["classifications"].append("gpt-header-at-lba1")
    return result


def inspect_archive(path: Path) -> list[dict]:
    rows = []
    if zipfile.is_zipfile(path):
        with zipfile.ZipFile(path) as archive:
            for item in archive.infolist():
                if item.is_dir():
                    continue
                with archive.open(item) as f:
                    header = f.read(4096)
                rows.append({
                    "path": item.filename,
                    "size_bytes": item.file_size,
                    "compressed_size_bytes": item.compress_size,
                    "classifications": classify_header(header, item.file_size),
                    "note": "Member content not fully hashed; inspect extracted file separately for SHA-256."
                })
        return rows
    if tarfile.is_tarfile(path):
        with tarfile.open(path, "r:*") as archive:
            for item in archive.getmembers():
                if not item.isfile():
                    continue
                f = archive.extractfile(item)
                header = f.read(4096) if f else b""
                rows.append({
                    "path": item.name,
                    "size_bytes": item.size,
                    "classifications": classify_header(header, item.size),
                    "note": "Member content not fully hashed; inspect extracted file separately for SHA-256."
                })
        return rows
    return [inspect_file(path)]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paths", nargs="+", help="Firmware files, ZIPs, TARs, or directories")
    parser.add_argument("--output", help="Write JSON report to this path instead of stdout")
    args = parser.parse_args()

    inputs: list[Path] = []
    for raw in args.paths:
        path = Path(raw)
        if path.is_dir():
            inputs.extend(p for p in sorted(path.rglob("*")) if p.is_file())
        elif path.is_file():
            inputs.append(path)
        else:
            parser.error(f"not found: {path}")

    report = {
        "schema_version": 1,
        "tool": "inspect_firmware_bundle",
        "safety": "No member is executed; archives are inventoried without extracting files.",
        "items": [],
    }
    for path in inputs:
        if zipfile.is_zipfile(path) or tarfile.is_tarfile(path):
            report["items"].append({
                "archive": str(path),
                "archive_sha256": inspect_file(path)["sha256"],
                "members": inspect_archive(path),
            })
        else:
            report["items"].append(inspect_file(path))

    rendered = json.dumps(report, indent=2, sort_keys=True)
    if args.output:
        Path(args.output).write_text(rendered + "\n", encoding="utf-8")
    else:
        print(rendered)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
