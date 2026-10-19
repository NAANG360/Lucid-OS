#!/usr/bin/env python3
"""Create a fail-closed manifest for tracked LucidOS custom payload files."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import stat
import sys

REQUIRED = [
    "system-tree/system/etc/init/init.lucid.rc",
    "system-tree/system/bin/lucid_desktop.sh",
    "system-tree/system/bin/lucid_firstboot.sh",
    "system-tree/system/bin/lucid_root_toggle.sh",
    "system-tree/system/etc/permissions/lucid-privapp-permissions.xml",
    "system-tree/system/priv-app/LucidSetup/LucidSetup.apk",
    "system-tree/system/usr/lucid/ubuntu-base-arm64.tar.gz",
    "theme/assets/lucid-wallpaper-dusk.jpg",
    "theme/assets/lucid-wallpaper-alpine.jpg",
    "theme/privapp-permissions-lucid.xml",
    "tools/generate_bootanimation.py",
    "docs/LUCIDOS_PRESERVATION_MANIFEST.md",
    "design/LUCIDOS_VISUAL_SYSTEM.md",
    "setup-app/AndroidManifest.xml",
    "setup-app/src/com/cosmos/setup/MainActivity.java",
    "tools/build_lucid_setup.sh",
    "system-tree/system/etc/selinux/plat_file_contexts",
]
TRACKED_CUSTOM_ROOTS = ["theme", "setup-app", "system-tree/system/usr/lucid"]
TRACKED_CUSTOM_PATHS = [
    "system-tree/system/etc/init/init.lucid.rc",
    "system-tree/system/bin/lucid_desktop.sh",
    "system-tree/system/bin/lucid_firstboot.sh",
    "system-tree/system/bin/lucid_root_toggle.sh",
    "system-tree/system/etc/permissions/lucid-privapp-permissions.xml",
    "system-tree/system/priv-app/LucidSetup/LucidSetup.apk",
    "system-tree/system/usr/lucid/ubuntu-base-arm64.tar.gz",
]
OPTIONAL_SEARCH_NAMES = {"init.lucid.rd", "init.lucid.rc"}


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def build_manifest(root: Path) -> dict:
    files = []
    missing = []
    for rel in REQUIRED:
        path = root / rel
        if not path.is_file():
            missing.append(rel)
            continue
        mode = stat.S_IMODE(path.stat().st_mode)
        files.append({"path": rel, "bytes": path.stat().st_size, "sha256": sha256(path), "mode": f"{mode:04o}"})
    # Hash every file in explicitly Lucid-owned roots, not just sentinel files.
    tracked_custom = []
    seen = {item["path"] for item in files}
    for rel in TRACKED_CUSTOM_PATHS:
        path = root / rel
        if path.is_file() and rel not in seen:
            tracked_custom.append({"path": rel, "bytes": path.stat().st_size, "sha256": sha256(path), "mode": f"{stat.S_IMODE(path.stat().st_mode):04o}"})
            seen.add(rel)
    for relroot in TRACKED_CUSTOM_ROOTS:
        base = root / relroot
        if base.exists():
            for path in sorted(base.rglob("*")):
                if not path.is_file():
                    continue
                rel = str(path.relative_to(root))
                if rel in seen:
                    continue
                tracked_custom.append({"path": rel, "bytes": path.stat().st_size, "sha256": sha256(path), "mode": f"{stat.S_IMODE(path.stat().st_mode):04o}"})
                seen.add(rel)
    init_matches = []
    for base in (root / "system-tree", root / "boot-ramdisk", root / "release-extracted"):
        if base.exists():
            for path in base.rglob("*"):
                if path.is_file() and path.name in OPTIONAL_SEARCH_NAMES:
                    init_matches.append(str(path.relative_to(root)))
    return {
        "schema": 1,
        "root": str(root),
        "required_count": len(REQUIRED),
        "present_required_count": len(files),
        "missing_required": missing,
        "required_files": files,
        "tracked_custom_roots": TRACKED_CUSTOM_ROOTS,
        "tracked_custom_file_count": len(tracked_custom),
        "tracked_custom_files": tracked_custom,
        "init_file_search": {"names": sorted(OPTIONAL_SEARCH_NAMES), "matches": sorted(set(init_matches)), "init_lucid_rd_found": any(Path(x).name == "init.lucid.rd" for x in init_matches)},
        "pass": not missing and all(x["bytes"] > 0 for x in files),
    }


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", type=Path, default=Path.cwd())
    ap.add_argument("--output", type=Path, default=Path("lucid-preservation-manifest.json"))
    args = ap.parse_args()
    root = args.root.resolve()
    result = build_manifest(root)
    out = args.output if args.output.is_absolute() else root / args.output
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"pass": result["pass"], "present_required_count": result["present_required_count"], "required_count": result["required_count"], "missing_required": result["missing_required"], "init_file_search": result["init_file_search"], "output": str(out)}, indent=2))
    return 0 if result["pass"] else 1

if __name__ == "__main__":
    sys.exit(main())
