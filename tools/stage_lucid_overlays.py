#!/usr/bin/env python3
"""Stage only explicitly Lucid-owned overlays onto an extracted Android base tree.

This deliberately does not copy the old system-tree wholesale. It preserves the
base framework, SystemUI, Settings, vendor and boot chain unless an individual
Lucid path is explicitly listed here.
"""
from __future__ import annotations
import argparse, hashlib, json, shutil, sys
from pathlib import Path

# Source paths are relative to the repository root; destinations are relative to
# the extracted Android filesystem root (the directory containing system/).
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
    h=hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda:f.read(1024*1024), b''): h.update(chunk)
    return h.hexdigest()

def inventory(src: Path, dst: Path):
    rows=[]
    sources=[src] if src.is_file() else sorted(p for p in src.rglob('*') if p.is_file())
    for s in sources:
        rel=s.relative_to(src) if src.is_dir() else Path(s.name)
        target=dst/rel if src.is_dir() else dst
        old_hash=digest(target) if target.is_file() else None
        new_hash=digest(s)
        rows.append({"source":str(s),"destination":str(target),"bytes":s.stat().st_size,"sha256":new_hash,"previous_sha256":old_hash,"action":"unchanged" if old_hash==new_hash else ("replace" if old_hash else "add")})
    return rows

def main() -> int:
    ap=argparse.ArgumentParser()
    ap.add_argument('--repo-root',type=Path,default=Path.cwd())
    ap.add_argument('--base-root',type=Path,required=True,help='Extracted base filesystem root containing system/')
    ap.add_argument('--report',type=Path,default=Path('lucid-overlay-plan.json'))
    ap.add_argument('--apply',action='store_true',help='Copy the listed Lucid paths; default is dry-run only')
    args=ap.parse_args()
    repo=args.repo_root.resolve(); base=args.base_root.resolve(); rows=[]; missing=[]
    for source_rel,dest_rel,required in OVERLAYS:
        source=repo/source_rel; destination=base/dest_rel
        if not source.exists():
            if required: missing.append(source_rel)
            continue
        rows.extend(inventory(source,destination))
    result={"schema":1,"mode":"apply" if args.apply else "dry-run","base_root":str(base),"overlay_count":len(rows),"missing_required_sources":missing,"actions":{"add":sum(r['action']=='add' for r in rows),"replace":sum(r['action']=='replace' for r in rows),"unchanged":sum(r['action']=='unchanged' for r in rows)},"overlays":rows,"pass":not missing and (base/'system').is_dir()}
    report=args.report if args.report.is_absolute() else repo/args.report
    report.parent.mkdir(parents=True,exist_ok=True); report.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:result[k] for k in ('mode','overlay_count','missing_required_sources','actions','pass','report') if k!='report'},indent=2))
    if not result['pass']: return 1
    if args.apply:
        for source_rel,dest_rel,required in OVERLAYS:
            source=repo/source_rel; destination=base/dest_rel
            if not source.exists(): continue
            if source.is_dir():
                for s in sorted(p for p in source.rglob('*') if p.is_file()):
                    target=destination/s.relative_to(source); target.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(s,target)
            else:
                destination.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(source,destination)
        print('Overlay files copied. Review the JSON report and reapply SELinux labels/file contexts before image creation.')
    return 0

if __name__=='__main__': sys.exit(main())
