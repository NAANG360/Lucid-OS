#!/usr/bin/env python3
"""Stage only explicitly Lucid-owned overlays onto an extracted Android base tree.

Dry-run is the default. Apply mode refuses symlinks in the source overlay and
in destination paths so a crafted extracted tree cannot redirect writes outside
the requested base root. This tool does not set Android ownership or SELinux
labels and does not build a flashable ZIP.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from pathlib import Path

OVERLAYS = [
    ("system-tree/system/etc/init/init.lucid.rc", "system/etc/init/init.lucid.rc", True),
    ("system-tree/system/bin/lucid_desktop.sh", "system/bin/lucid_desktop.sh", True),
    ("system-tree/system/bin/lucid_firstboot.sh", "system/bin/lucid_firstboot.sh", True),
    ("system-tree/system/bin/lucid_root_toggle.sh", "system/bin/lucid_root_toggle.sh", True),
    ("system-tree/system/etc/permissions/lucid-privapp-permissions.xml", "system/etc/permissions/lucid-privapp-permissions.xml", True),
    ("system-tree/system/priv-app/LucidSetup/LucidSetup.apk", "system/priv-app/LucidSetup/LucidSetup.apk", True),
    ("system-tree/system/usr/lucid", "system/usr/lucid", True),
]


def digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def reject_symlink_components(path: Path, stop: Path) -> None:
    """Reject symlinks from stop through path, including path itself if present."""
    stop = stop.resolve()
    try:
        rel = path.relative_to(stop)
    except ValueError as exc:
        raise ValueError(f"path escapes expected root: {path}") from exc
    current = stop
    if current.is_symlink():
        raise ValueError(f"root must not be a symlink: {current}")
    for part in rel.parts:
        current = current / part
        if current.is_symlink():
            raise ValueError(f"symlink paths are not supported: {current}")


def source_files(src: Path) -> list[Path]:
    if src.is_symlink():
        raise ValueError(f"source symlink is not supported: {src}")
    if src.is_file():
        return [src]
    if not src.is_dir():
        return []
    result = []
    for path in sorted(src.rglob("*")):
        if path.is_symlink():
            raise ValueError(f"source tree contains symlink: {path}")
        if path.is_file():
            result.append(path)
    return result


def inventory(src: Path, dst: Path):
    rows = []
    for source in source_files(src):
        rel = source.relative_to(src) if src.is_dir() else Path(source.name)
        target = dst / rel if src.is_dir() else dst
        old_hash = digest(target) if target.is_file() and not target.is_symlink() else None
        new_hash = digest(source)
        rows.append({
            "source": str(source),
            "destination": str(target),
            "bytes": source.stat().st_size,
            "sha256": new_hash,
            "previous_sha256": old_hash,
            "mode": f"{source.stat().st_mode & 0o7777:04o}",
            "action": "unchanged" if old_hash == new_hash else ("replace" if old_hash else "add"),
        })
    return rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", type=Path, default=Path.cwd())
    parser.add_argument("--base-root", type=Path, required=True,
                        help="Extracted base filesystem root containing system/")
    parser.add_argument("--report", type=Path, default=Path("lucid-overlay-plan.json"))
    parser.add_argument("--apply", action="store_true",
                        help="Copy the listed Lucid paths; default is dry-run only")
    args = parser.parse_args()
    repo = args.repo_root.resolve()
    base = args.base_root.resolve()
    rows, missing = [], []
    try:
        if not (base / "system").is_dir():
            raise ValueError(f"base root does not contain system/: {base}")
        reject_symlink_components(base / "system", base)
        for source_rel, dest_rel, required in OVERLAYS:
            source = repo / source_rel
            destination = base / dest_rel
            if not source.exists() and not source.is_symlink():
                if required:
                    missing.append(source_rel)
                continue
            reject_symlink_components(source, repo)
            reject_symlink_components(destination, base)
            rows.extend(inventory(source, destination))
    except (OSError, ValueError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2

    result = {
        "schema": 2,
        "mode": "apply" if args.apply else "dry-run",
        "base_root": str(base),
        "overlay_count": len(rows),
        "missing_required_sources": missing,
        "actions": {action: sum(row["action"] == action for row in rows)
                    for action in ("add", "replace", "unchanged")},
        "overlays": rows,
        "pass": not missing,
        "limitations": [
            "Does not create a flashable ROM or modify boot/vendor/framework/SystemUI.",
            "Android uid/gid ownership, modes, capabilities and SELinux labels must be set and validated by the image builder.",
            "Symlinks are intentionally rejected; convert required payloads to regular files or add a reviewed symlink policy."
        ],
    }
    report = args.report if args.report.is_absolute() else repo / args.report
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: result[key] for key in
                      ("mode", "overlay_count", "missing_required_sources", "actions", "pass")}, indent=2))
    if not result["pass"]:
        return 1

    if args.apply:
        try:
            for source_rel, dest_rel, required in OVERLAYS:
                source = repo / source_rel
                if not source.exists():
                    continue
                destination = base / dest_rel
                reject_symlink_components(destination, base)
                if source.is_dir():
                    for item in source_files(source):
                        target = destination / item.relative_to(source)
                        reject_symlink_components(target, base)
                        target.parent.mkdir(parents=True, exist_ok=True)
                        shutil.copy2(item, target)
                else:
                    destination.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(source, destination)
        except (OSError, ValueError) as exc:
            print(f"ERROR while applying overlays: {exc}", file=sys.stderr)
            return 2
        print("Overlay files copied. Reapply Android ownership and SELinux labels before image creation.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
