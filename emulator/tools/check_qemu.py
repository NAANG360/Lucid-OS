#!/usr/bin/env python3
"""Check the free generic ARM64 QEMU lab prerequisites."""
import json, shutil, subprocess, sys
from pathlib import Path
root=Path(__file__).resolve().parents[1]
profile=json.loads((root/"config/lavender-profile.json").read_text())
qemu=shutil.which("qemu-system-aarch64")
result={"target":profile["target"],"generic_qemu_reference":profile["generic_qemu_reference"],"qemu_system_aarch64":qemu,"qemu_version":None,"sdm660_specific_machine":False}
if qemu:
    p=subprocess.run([qemu,"--version"],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=10)
    result["qemu_version"]=p.stdout.splitlines()[0] if p.stdout else "unknown"
    h=subprocess.run([qemu,"-machine","help"],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=10)
    result["virt_machine_available"]="virt" in h.stdout
else:
    result["virt_machine_available"]=False
print(json.dumps(result,indent=2))
if not qemu:
    print("QEMU is not installed. Install qemu-system-arm on a Linux host to launch a guest.",file=sys.stderr)
    sys.exit(2)
if not result["virt_machine_available"]:
    print("QEMU binary does not list the generic virt machine.",file=sys.stderr)
    sys.exit(3)
