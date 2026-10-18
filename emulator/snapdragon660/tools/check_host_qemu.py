#!/usr/bin/env python3
"""Record host QEMU capability facts without claiming they emulate SDM660."""
import argparse, json
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument("--machines", required=True)
p.add_argument("--cpus", required=True)
p.add_argument("--output", required=True)
a = p.parse_args()
machines = Path(a.machines).read_text(errors="replace")
cpus = Path(a.cpus).read_text(errors="replace")
report = {
  "qemu_machine_help_contains_virt": "virt" in machines,
  "qemu_native_sdm660_machine_present": any(x in machines.lower() for x in ("sdm660", "lavender")),
  "cpu_models": [line.strip() for line in cpus.splitlines() if line.strip()][:500],
  "warning": "QEMU host machine/CPU capability inventory only; does not imply SDM660 emulation.",
}
Path(a.output).write_text(json.dumps(report, indent=2) + "\n")
print(json.dumps(report, indent=2))
