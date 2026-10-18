#!/usr/bin/env python3
"""Validate and emit a machine-readable report for the SDM660 variant scaffold."""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROFILE = ROOT / "profile.json"
DTS = ROOT / "device-tree" / "sdm660-lavender.dts"
OUT = ROOT / "platform-description.json"


def build():
    profile = json.loads(PROFILE.read_text(encoding="utf-8"))
    text = DTS.read_text(encoding="utf-8")
    assert profile["soc"]["part"] == "SDM660"
    assert profile["implementation"]["qemu_machine_type"] == "sdm660-lavender"
    assert 'compatible = "xiaomi,lavender-sdm660", "qcom,sdm660";' in text
    assert "NOT a silicon-accurate device tree" in text
    assert "reg = <0x0 0x80000000 0x0 0x10000000>;" in text
    assert "status = \"disabled\";" in text
    return {
        "schema_version": 1,
        "profile": profile,
        "device_tree_scaffold": str(DTS.relative_to(ROOT)),
        "device_tree_scaffold_status": "disabled-incomplete-not-bootable",
        "qemu_machine_type": "sdm660-lavender",
        "warning": "This is not yet a native SDM660 QEMU machine and cannot boot stock lavender Android.",
        "evidence_policy": "No unverified Qualcomm MMIO addresses or IRQs are claimed.",
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    result = build()
    rendered = json.dumps(result, indent=2) + "\n"
    if args.check:
        if not OUT.exists() or OUT.read_text(encoding="utf-8") != rendered:
            OUT.write_text(rendered, encoding="utf-8")
        print("SDM660 variant scaffold validated")
    else:
        print(rendered, end="")


if __name__ == "__main__":
    main()
