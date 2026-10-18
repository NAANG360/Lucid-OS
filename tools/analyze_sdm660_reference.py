#!/usr/bin/env python3
"""Inventory public SDM660/lavender source references for ROM debugging."""
from __future__ import annotations
import sys, json, hashlib, re
from pathlib import Path
from datetime import datetime, timezone

ROOT = Path(sys.argv[1] if len(sys.argv) > 1 else "reference-work")
OUT = Path(sys.argv[2] if len(sys.argv) > 2 else "report/sdm660-reference")
OUT.mkdir(parents=True, exist_ok=True)
TEXT_EXT = {".dts", ".dtsi", ".mk", ".bp", ".prop", ".rc", ".fstab", ".conf", ".config", ".txt", ".xml", ".sh", ".yaml", ".yml", ".md", ".json", ".te", ".cil", ".h"}
PATTERNS = {
 "device_tree_compatible": re.compile(r'compatible\s*=\s*("[^"]*(?:qcom|qualcomm|sdm660|sdm630|sdm636|lavender)[^"]*")', re.I),
 "partition_and_dynamic": re.compile(r".{0,100}(?:super_partition|dynamic_partitions|BOARD_.*PARTITION|system_[a-z_]*size|vendor_[a-z_]*size|product_[a-z_]*size|cust|by-name).{0,140}", re.I),
 "boot_and_fstab": re.compile(r".{0,100}(?:fstab|first_stage_mount|logical|slotselect|avb|verity|dtbo|bootdevice|/dev/block).{0,140}", re.I),
 "firmware_and_hal": re.compile(r".{0,100}(?:proprietary-files|vendor\.firmware|firmware/|qti[-_](?:hardware|hal)|android\.hardware\.(?:camera|graphics|audio|sensors)|lib.*(?:adreno|qmi|dsp)).{0,140}", re.I),
 "kernel_config": re.compile(r"^(?:CONFIG_(?:ARCH_QCOM|QCOM|MSM|ANDROID|ARM64|EFI|OF|IOMMU|DRM|MACH|USB|MMC|EXT4|DM_|BLK_DEV_DM|FW_LOADER|REMOTEPROC|RPMSG|REGULATOR|COMMON_CLK|PINCTRL).*)$", re.M),
}
def text(p):
 try: return p.read_text(errors="replace")[:180000]
 except OSError: return ""
def rel(p):
 try: return str(p.relative_to(ROOT))
 except ValueError: return str(p)
files = sorted(p for p in ROOT.rglob("*") if p.is_file() and ".git" not in p.parts and (p.suffix.lower() in TEXT_EXT or p.name in {"BoardConfig.mk","AndroidProducts.mk","fstab.qcom","defconfig"}))
inventory, matches = [], {k: [] for k in PATTERNS}
for p in files:
 s=text(p)
 if not s: continue
 inventory.append({"path":rel(p),"bytes":p.stat().st_size,"sha256":hashlib.sha256(s.encode()).hexdigest()})
 for key, pattern in PATTERNS.items():
  for m in pattern.finditer(s):
   item={"file":rel(p),"match":re.sub(r"\s+"," ",m.group(0).strip())[:260]}
   if item not in matches[key]: matches[key].append(item)
for k in matches: matches[k]=matches[k][:1200]
repos=[]
for p in ROOT.iterdir():
 if not p.is_dir() or not (p/".git").exists(): continue
 config=text(p/".git/config")
 url=next((x.strip().split(" = ",1)[1] for x in config.splitlines() if x.strip().startswith("url = ")),"unknown")
 repos.append({"directory":p.name,"url":url,"head":text(p/".git/HEAD").strip()})
report={"generated_utc":datetime.now(timezone.utc).isoformat(),"scope_note":"Source inventory only; not a hardware emulator and not proof of boot compatibility.","repositories":repos,"text_file_count":len(inventory),"files":inventory,"matches":matches}
(OUT/"sdm660-reference.json").write_text(json.dumps(report,indent=2)+"\n")
md=["# SDM660 / Redmi Note 7 source-reference audit","",f"- Generated UTC: {report['generated_utc']}",f"- Text files inventoried: {len(inventory)}","- Scope: source inventory only; not a hardware emulator and not proof of boot compatibility.","","## Repositories",""]
md += [f"- **{r['directory']}** — {r['url']} — {r['head']}" for r in repos]
md += ["","## Match counts",""]+[f"- {k}: {len(v)}" for k,v in matches.items()]
for key in ("partition_and_dynamic","boot_and_fstab","device_tree_compatible","kernel_config","firmware_and_hal"):
 md += ["",f"## {key.replace('_',' ').title()}",""]
 md += [f"- {x['file']}: {x['match']}" for x in matches[key][:300]]
md += ["","## Scanned text files",""]+[f"- {x['path']} ({x['bytes']} bytes; scanned-text SHA-256 {x['sha256']})" for x in inventory]
(OUT/"README.md").write_text("\n".join(md)+"\n")
print(f"Scanned {len(inventory)} text files across {len(repos)} repositories.")
