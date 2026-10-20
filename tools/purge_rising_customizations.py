#!/usr/bin/env python3
"""Remove explicitly identifiable RisingOS branding/apps from extracted ROM trees.

This is deliberately conservative: it never removes Android framework, SystemUI,
Settings, device/vendor files, or an APK merely because its contents mention Rising.
Only package IDs/app labels or app/overlay filenames explicitly identifying Rising
are removal candidates. Every removal and every unresolved candidate is reported.
"""
from __future__ import annotations
import argparse, json, re, subprocess
from pathlib import Path

PARTITIONS = ("system", "product", "system_ext", "vendor", "odm")
RISING = re.compile(r"rising(?:os)?", re.I)
PROP_KEY = re.compile(r"^(?:ro\.(?:rising|risingos)\.[^=]+)=.*$")
SAFE_APP_ROOTS = ("app", "priv-app", "overlay", "framework-res/overlay")

def apk_identity(aapt: str, apk: Path) -> tuple[str, str]:
    p = subprocess.run([aapt, "dump", "badging", str(apk)], text=True,
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=30)
    if p.returncode:
        return "", ""
    package = re.search(r"^package: name='([^']+)'", p.stdout, re.M)
    label = re.search(r"^application-label:'([^']*)'", p.stdout, re.M)
    return (package.group(1) if package else "", label.group(1) if label else "")

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--base-root", type=Path, required=True)
    ap.add_argument("--aapt", required=True)
    ap.add_argument("--report", type=Path, required=True)
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args()
    root = args.base_root.resolve()
    report = {"schema":1,"mode":"apply" if args.apply else "dry-run","removed":[],"branding_edits":[],"unresolved":[]}
    app_candidates = []
    for part in PARTITIONS:
        tree = root / part
        if not tree.is_dir():
            report["unresolved"].append(f"missing partition tree: {part}")
            continue
        for apk in tree.rglob("*.apk"):
            if apk.is_symlink():
                continue
            rel = apk.relative_to(tree).as_posix()
            if not any(seg in SAFE_APP_ROOTS for seg in Path(rel).parts[:-1]):
                continue
            package, label = apk_identity(args.aapt, apk)
            if RISING.search(apk.name) or RISING.search(package) or RISING.search(label):
                app_candidates.append((apk, part, rel, package, label))
        for prop in list(tree.rglob("*.prop")):
            if prop.is_symlink() or prop.name not in ("build.prop", "default.prop"):
                continue
            try: lines = prop.read_text(errors="replace").splitlines()
            except OSError as e:
                report["unresolved"].append(f"cannot read {prop}: {e}")
                continue
            kept = [x for x in lines if not PROP_KEY.match(x)]
            removed = [x for x in lines if PROP_KEY.match(x)]
            if removed:
                report["branding_edits"].append({"file":str(prop),"removed_lines":removed})
                if args.apply: prop.write_text("\n".join(kept)+"\n")
        for p in tree.rglob("*"):
            if p.is_symlink() or not p.is_file(): continue
            rel = p.relative_to(tree).as_posix()
            if (RISING.search(p.name) and ("/overlay/" in "/"+rel or p.suffix.lower() in (".rc",".xml",".json"))) and not p.name.endswith(".apk"):
                report["unresolved"].append(f"Rising-named non-APK configuration requires review: {part}/{rel}")
    for apk, part, rel, package, label in app_candidates:
        report["removed"].append({"partition":part,"path":rel,"package":package,"label":label})
        if args.apply:
            # Remove only the app's own directory when its basename matches the APK
            # stem and all files inside are app payloads; otherwise remove the APK alone.
            parent = apk.parent
            if parent.name == apk.stem and parent != (root / part):
                import shutil
                shutil.rmtree(parent)
            else:
                apk.unlink()
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2)+"\n")
    print(json.dumps({"mode":report["mode"],"removed_apps":len(report["removed"]),
                      "branding_files_edited":len(report["branding_edits"]),
                      "unresolved_review_items":len(report["unresolved"])},indent=2))
    return 1 if report["unresolved"] else 0

if __name__ == "__main__":
    raise SystemExit(main())
