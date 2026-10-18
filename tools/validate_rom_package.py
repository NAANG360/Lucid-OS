#!/usr/bin/env python3
"""Static validator for an Android OTA ZIP; reports risks without modifying it."""
import sys, zipfile, re, hashlib
from pathlib import Path
if len(sys.argv) != 3: raise SystemExit("usage: validate_rom_package.py ROM.zip REPORT.md")
rom, report = Path(sys.argv[1]), Path(sys.argv[2])
if not rom.is_file(): raise SystemExit(f"ROM ZIP not found: {rom}")
warnings, checks, details = [], [], []
def check(label, ok, detail): checks.append((label, bool(ok), detail))
def read_member(z, name, limit=12*1024*1024):
    try:
        info=z.getinfo(name)
        if info.file_size>limit:
            warnings.append(f"{name}: skipped oversized text member ({info.file_size} bytes)"); return ""
        return z.read(name).decode("utf-8","replace")
    except KeyError: return ""
with zipfile.ZipFile(rom) as z:
    bad=z.testzip(); check("ZIP CRC",bad is None,"all entries passed CRC" if bad is None else f"corrupt member: {bad}")
    names=set(z.namelist())
    check("boot.img present","boot.img" in names,"boot.img present" if "boot.img" in names else "boot.img missing")
    for p in ("system","vendor","product","odm","system_ext"):
        transfer=f"{p}.transfer.list"; payload=f"{p}.new.dat.br"; present=transfer in names and payload in names
        check(f"{p} OTA payload",present,f"{transfer} and {payload} present" if present else f"missing {transfer if transfer not in names else payload}")
        if transfer not in names: continue
        lines=[x.strip() for x in read_member(z,transfer).splitlines() if x.strip()]
        if len(lines)<4: warnings.append(f"{p}: transfer list has fewer than four header lines"); continue
        try:
            version,header,stash,maxstash=map(int,lines[:4]); totals={"new":0,"zero":0,"erase":0}; ranges=[]; maxend=0
            for ln,line in enumerate(lines[4:],5):
                cmd,sep,rest=line.partition(" ")
                if not sep or cmd not in totals: continue
                nums=[int(x) for x in rest.split(",") if x]
                if not nums: continue
                n,vals=nums[0],nums[1:]
                if n!=len(vals) or n%2: raise ValueError(f"bad range list at line {ln}")
                for start,end in zip(vals[::2],vals[1::2]):
                    if start<0 or end<start: raise ValueError(f"invalid range at line {ln}")
                    totals[cmd]+=end-start; maxend=max(maxend,end); ranges.append((start,end,cmd))
            check(f"{p} transfer header",header==totals["new"]+totals["zero"],f"header={header}; new={totals['new']}; zero={totals['zero']}; erase={totals['erase']}")
            check(f"{p} nonempty write ranges",totals["new"]>0,f"new blocks={totals['new']}")
            overlap=None
            for command in totals:
                ordered=sorted((a,b) for a,b,kind in ranges if kind==command)
                for a,b in zip(ordered,ordered[1:]):
                    if a[1]>b[0]: overlap=(command,a,b); break
                if overlap: break
            check(f"{p} non-overlapping same-command ranges",overlap is None,"no same-command overlaps" if overlap is None else f"overlap {overlap}")
            details.append(f"- {p}: max block end {maxend}; new {totals['new']}; zero {totals['zero']}; erase {totals['erase']}; header {header}.")
        except (ValueError,OverflowError) as e: warnings.append(f"{p}: transfer-list parse error: {e}")
    updater="\n".join(read_member(z,n) for n in names if n.endswith("updater-script"))
    fstabs="\n".join(read_member(z,n) for n in names if "fstab" in n.lower() and not n.endswith("/"))
    alltext="\n".join(read_member(z,n) for n in names if n.endswith((".prop",".rc",".xml",".txt",".list",".script")))
    check("dynamic partition evidence","system" in alltext.lower() and ("super" in updater.lower() or "dynamic" in alltext.lower()),"searched updater scripts and text metadata")
    cust=bool(re.search(r"\bcust\b",updater+"\n"+fstabs,re.I))
    check("cust partition reference",cust,"cust reference found" if cust else "no cust reference in updater/fstab files")
    check("installer metadata",any(n.startswith("META-INF/") for n in names),"META-INF present" if any(n.startswith("META-INF/") for n in names) else "META-INF missing")
    sha=hashlib.sha256(rom.read_bytes()).hexdigest()
lines=["# ROM package static validation","",f"- File: {rom.name}",f"- Size: {rom.stat().st_size} bytes",f"- SHA-256: {sha}","- Static ZIP/transfer-list check only; not a successful boot test.","","## Checks",""]
lines += [f"- {'PASS' if ok else 'WARN'} — {label}: {detail}" for label,ok,detail in checks]
lines += ["","## Partition transfer summaries",""]+details+["","## Warnings",""]
lines += [f"- {w}" for w in warnings] if warnings else ["- No parser warnings generated."]
report.parent.mkdir(parents=True,exist_ok=True); report.write_text("\n".join(lines)+"\n")
print(f"Checked {len(checks)} invariants; emitted {len(warnings)} parser warnings to {report}")
