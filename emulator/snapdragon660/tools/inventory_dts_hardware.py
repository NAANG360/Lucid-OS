#!/usr/bin/env python3
"""Extract an evidence-backed inventory from upstream Qualcomm SDM660 DTSI.

This deliberately does not guess the QEMU machine's physical map. It records
only literal DTS properties and source provenance, preserving raw cell tuples
because address/size cell widths vary across nodes and include files.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import re
from pathlib import Path

NODE_RE = re.compile(r"(?:(?P<label>[A-Za-z_][\w-]*)\s*:\s*)?(?P<name>[A-Za-z_][\w,.-]*@[\w,.+-]+|[A-Za-z_][\w,.-]*)\s*\{")
PROP_RE = re.compile(r"(?m)^\s*(?P<key>[A-Za-z_#][\w,#-]*)\s*=\s*(?P<value>.*?);")
STR_RE = re.compile(r'"([^"]*)"')
CELL_RE = re.compile(r"<([^>]*)>")

def node_blocks(text: str):
    """Yield shallow node blocks by balanced braces, ignoring braces in comments/strings."""
    # DTS comments are stripped while preserving quoted strings and line count.
    clean = re.sub(r"/\*.*?\*/", lambda m: "\n" * m.group(0).count("\n"), text, flags=re.S)
    clean = re.sub(r"//[^\n]*", "", clean)
    i = 0
    while i < len(clean):
        m = NODE_RE.search(clean, i)
        if not m:
            break
        start = m.end()
        depth, j, quoted, escaped = 1, start, False, False
        while j < len(clean) and depth:
            c = clean[j]
            if quoted:
                if escaped: escaped = False
                elif c == "\\": escaped = True
                elif c == '"': quoted = False
            else:
                if c == '"': quoted = True
                elif c == "{": depth += 1
                elif c == "}": depth -= 1
            j += 1
        body = clean[start:j-1] if depth == 0 else clean[start:]
        props = {}
        for p in PROP_RE.finditer(body):
            props[p.group("key")] = p.group("value").strip()
        yield {"label": m.group("label"), "node": m.group("name"), "properties": props}
        i = j

def parse_cells(value: str):
    out = []
    for group in CELL_RE.findall(value):
        cells = []
        for token in group.split():
            token = token.rstrip(",")
            try: cells.append(int(token, 0))
            except ValueError: cells.append(token)
        out.append(cells)
    return out

def inventory(text: str, source: str):
    nodes = []
    for item in node_blocks(text):
        p = item["properties"]
        compatible = STR_RE.findall(p.get("compatible", ""))
        if not ({"reg", "interrupts", "compatible"} & p.keys()):
            continue
        nodes.append({
            "label": item["label"], "node": item["node"],
            "compatible": compatible,
            "reg_cells": parse_cells(p.get("reg", "")),
            "interrupt_cells": parse_cells(p.get("interrupts", "")),
            "clock_names": STR_RE.findall(p.get("clock-names", "")),
            "reset_names": STR_RE.findall(p.get("reset-names", "")),
            "status": STR_RE.findall(p.get("status", "")) or ["okay (implicit)"],
            "source": source,
        })
    return nodes

def build_report(inputs: list[tuple[str, str]]):
    all_nodes = []
    source_meta = []
    for source, text in inputs:
        source_meta.append({"path": source, "sha256": hashlib.sha256(text.encode()).hexdigest(), "bytes": len(text.encode())})
        all_nodes.extend(inventory(text, source))
    def find(label):
        return [n for n in all_nodes if n["label"] == label or n["node"].split("@")[0] == label]
    return {
        "schema_version": 1,
        "purpose": "public-source evidence inventory; not a machine implementation",
        "target": {"soc": "Qualcomm SDM660", "device": "Xiaomi Redmi Note 7", "codename": "lavender"},
        "sources": source_meta,
        "counts": {
            "nodes_with_hardware_properties": len(all_nodes),
            "nodes_with_reg": sum(bool(n["reg_cells"]) for n in all_nodes),
            "nodes_with_interrupts": sum(bool(n["interrupt_cells"]) for n in all_nodes),
            "unique_compatible_strings": len({c for n in all_nodes for c in n["compatible"]}),
        },
        "boot_critical_candidates": {
            "interrupt_controller": find("intc"),
            "uart": [n for n in all_nodes if any("uart" in c.lower() for c in n["compatible"]) or "uart" in n["node"].lower()],
            "storage": [n for n in all_nodes if any(k in (n["node"] + " " + " ".join(n["compatible"])).lower() for k in ("sdhc", "sdhci", "ufs", "emmc"))],
            "timer": [n for n in all_nodes if "timer" in n["node"].lower() or any("timer" in c.lower() for c in n["compatible"])],
            "clocks_and_resets": [n for n in all_nodes if any(k in " ".join(n["compatible"]).lower() for k in ("gcc", "clock-controller", "reset"))],
        },
        "all_nodes": all_nodes,
        "limitations": [
            "DTS include files and overlays may add or override nodes; this report is not a fully preprocessed device tree.",
            "Cell tuples are preserved literally; converting them into physical addresses requires the applicable #address-cells/#size-cells and ranges at each bus.",
            "No MMIO map is synthesized and no unsupported register behavior is inferred.",
            "A DTS inventory alone does not provide boot firmware, secure-world behavior, proprietary GPU/DSP/modem firmware, or a working emulator."
        ]
    }

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--input", action="append", required=True, metavar="SOURCE=PATH", help="DTS/DTSI file and its source identifier")
    ap.add_argument("--output", required=True)
    args = ap.parse_args()
    inputs = []
    for spec in args.input:
        if "=" not in spec: ap.error("--input must be SOURCE=PATH")
        source, path = spec.split("=", 1)
        inputs.append((source, Path(path).read_text(encoding="utf-8", errors="replace")))
    report = build_report(inputs)
    Path(args.output).write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps({"output": args.output, "counts": report["counts"], "source_count": len(inputs)}, indent=2))

if __name__ == "__main__":
    main()
