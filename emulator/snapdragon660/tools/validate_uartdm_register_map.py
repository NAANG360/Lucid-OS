#!/usr/bin/env python3
"""Check the QEMU UARTDM register constants against the pinned lavender kernel header."""
from __future__ import annotations

import argparse
import re
from pathlib import Path

EXPECTED = {
    "UARTDM_DMEN": "UART_DM_DMEN",
    "UARTDM_SR": "UART_DM_SR",
    "UARTDM_CR": "UART_DM_CR",
    "UARTDM_TF": "UART_DM_TF",
    "UARTDM_RF": "UART_DM_RF",
    "UARTDM_CSR": "UART_DM_CSR",
    "UARTDM_IMR": "UART_DM_IMR",
}
HEX_DEFINE = re.compile(r"^\s*#define\s+(\w+)\s+(0x[0-9a-fA-F]+|[0-9]+)\b", re.M)
ENUM_ASSIGN = re.compile(r"^\s*(UART_DM_(?:DMEN|SR|CR|TF|RF|CSR|IMR))\s*=\s*(0x[0-9a-fA-F]+|[0-9]+)\s*,?", re.M)


def defines(path: Path) -> dict[str, int]:
    content = path.read_text()
    values = {name: int(value, 0) for name, value in HEX_DEFINE.findall(content)}
    values.update({name: int(value, 0) for name, value in ENUM_ASSIGN.findall(content)})
    return values


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--kernel-header", required=True, type=Path)
    parser.add_argument("--qemu-source", required=True, type=Path)
    parser.add_argument("--kernel-dtsi", required=True, type=Path,
                        help="Pinned SDM660 SoC DTSI containing the UARTDM reg tuple")
    args = parser.parse_args()
    reference = defines(args.kernel_header)
    implementation = defines(args.qemu_source)
    errors = []
    for qemu_name, kernel_name in EXPECTED.items():
        if kernel_name not in reference:
            errors.append(f"reference header is missing {kernel_name}")
        elif qemu_name not in implementation:
            errors.append(f"QEMU model is missing {qemu_name}")
        elif reference[kernel_name] != implementation[qemu_name]:
            errors.append(
                f"{qemu_name}=0x{implementation[qemu_name]:x}, "
                f"but pinned kernel says {kernel_name}=0x{reference[kernel_name]:x}"
            )
    if errors:
        print("\n".join(errors))
        return 1
    print(f"validated {len(EXPECTED)} UARTDM register offsets against pinned lavender kernel source")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
