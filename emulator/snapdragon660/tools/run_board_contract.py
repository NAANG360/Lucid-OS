#!/usr/bin/env python3
"""Run deterministic SDM660 board-contract smoke scenarios.

This does not launch QEMU. It is the executable peripheral-model test harness
used until the corresponding models are wired into a native QEMU machine.
"""
import argparse
import json
from pathlib import Path
import sys

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "model"))
from peripherals import SDM660BoardModel

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--console", default="SDM660 lavender model ready\n")
    parser.add_argument("--ram-mib", type=int, default=4096)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    if args.ram_mib < 128 or args.ram_mib > 16384:
        parser.error("--ram-mib must be between 128 and 16384")
    board = SDM660BoardModel(ram_size=args.ram_mib * 1024 * 1024)
    board.set_clock("console.reference", True)
    board.set_reset("console", False)
    board.guest_console_write(args.console.encode("utf-8"))
    result = board.snapshot()
    result["console_text"] = board.uart.output().decode("utf-8", errors="replace")
    rendered = json.dumps(result, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered)
    print(rendered, end="")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
