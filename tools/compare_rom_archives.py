#!/usr/bin/env python3
"""Compare original LineageOS and derived LUCID OS ZIPs without extracting them.
Usage: python3 tools/compare_rom_archives.py ORIGINAL.zip LUCIDOS.zip [REPORT.md]
This records evidence; it does not certify bootability.
"""
import hashlib, sys, zipfile
from pathlib import Path

IMPORTANT = ('boot.img','dtbo.img','vbmeta.img','super_dummy.img','system.new.dat.br','system.transfer.list','vendor.new.dat.br','vendor.transfer.list','product.new.dat.br','product.transfer.list','odm.new.dat.br','odm.transfer.list','system_ext.new.dat.br','system_ext.transfer.list','META-INF/com/google/android/updater-script')

def sha256(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b''): h.update(chunk)
    return h.hexdigest()

def inspect(path):
    out = {'path': str(path), 'bytes': Path(path).stat().st_size, 'sha256': sha256(path)}
    with zipfile.ZipFile(path) as z:
        bad = z.testzip(); infos = {i.filename: i.file_size for i in z.infolist()}
        out.update(valid_zip=bad is None, first_bad_entry=bad, entries=infos, entry_count=len(infos))
        out['build_props'] = {}
        for name, size in infos.items():
            if name.lower().endswith('build.prop') and size <= 2*1024*1024:
                text = z.read(name).decode('utf-8','replace')
                keys = ('ro.build.version.release=', 'ro.build.version.sdk=', 'ro.lineage.version=', 'ro.build.fingerprint=', 'ro.product.device=', 'ro.product.vendor.device=')
                out['build_props'][name] = [line for line in text.splitlines() if line.startswith(keys)]
        out['scripts'] = {}
        for name in infos:
            if name.endswith('META-INF/com/google/android/updater-script'):
                lines = z.read(name).decode('utf-8','replace').splitlines()
                keys = ('lavender','super_dummy','block_image_update','delete','format','product','system_ext','vendor')
                out['scripts'][name] = [line.strip() for line in lines if any(k in line.lower() for k in keys)][:250]
    return out

def size(n): return 'MISSING' if n is None else f'{n:,} bytes ({n/1048576:.2f} MiB)'

def report(a,b):
    lines = ['# Original LineageOS vs LUCID OS archive comparison', '', '> Archive inspection only; this is not a bootability certification.', '',
    '| Field | Original | LUCID |','|---|---|---|',
    f"| File | {a['path']} | {b['path']} |", f"| Size | {size(a['bytes'])} | {size(b['bytes'])} |",
    f"| SHA-256 | `{a['sha256']}` | `{b['sha256']}` |",
    f"| ZIP integrity | {'PASS' if a['valid_zip'] else 'FAIL'} | {'PASS' if b['valid_zip'] else 'FAIL'} |",
    f"| Entry count | {a['entry_count']} | {b['entry_count']} |", '', '## Key payloads', '', '| Path | Original | LUCID |','|---|---:|---:|']
    for name in IMPORTANT:
        x,y=a['entries'].get(name),b['entries'].get(name)
        if x is not None or y is not None: lines.append(f'| `{name}` | {size(x)} | {size(y)} |')
    an,bn=set(a['entries']),set(b['entries'])
    lines += ['', '## Archive path differences', '', f'- Original-only entries: {len(an-bn)}', f'- LUCID-only entries: {len(bn-an)}', f'- Shared paths: {len(an&bn)}', '', '## Build properties', '']
    for label,item in (('Original LineageOS',a),('LUCID OS',b)):
        lines += [f'### {label}', '']
        if not item['build_props']: lines += ['No build.prop found.', '']
        for name,props in item['build_props'].items(): lines += [f'**{name}**', ''] + [f'- `{p}`' for p in props] + ['']
    lines += ['## Installer clues', '']
    for label,item in (('Original LineageOS',a),('LUCID OS',b)):
        lines += [f'### {label}', '']
        if not item['scripts']: lines += ['No updater-script found.', '']
        for name,selected in item['scripts'].items(): lines += [f'**{name}**',''] + [f'- `{s}`' for s in selected] + ['']
    lines += ['## Safety notes', '', '- Incremental OTA omissions may be intentional but must be evaluated against base-image dependencies.', '- ZIP CRC validity does not establish device compatibility or bootability.', '- Before flashing, verify transfer-list reconstruction, ext4 integrity, dynamic-partition metadata, AVB, and device boot logs.', '']
    return '\n'.join(lines)

def main():
    if len(sys.argv) not in (3,4): raise SystemExit('usage: compare_rom_archives.py ORIGINAL.zip LUCIDOS.zip [REPORT.md]')
    result=report(inspect(sys.argv[1]),inspect(sys.argv[2]))
    if len(sys.argv)==4: Path(sys.argv[3]).write_text(result,encoding='utf-8')
    else: print(result)
if __name__=='__main__': main()
