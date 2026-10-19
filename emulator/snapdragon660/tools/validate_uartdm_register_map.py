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
    "UARTDM_ISR": "UART_DM_ISR",
}
EXPECTED_VALUES = {"UARTDM_CR_CLEAR_TX_READY": "CLEAR_TX_READY"}
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
    for qemu_name, kernel_name in EXPECTED_VALUES.items():
        if kernel_name not in reference:
            errors.append(f"reference header is missing {kernel_name}")
        elif qemu_name not in implementation:
            errors.append(f"QEMU model is missing {qemu_name}")
        elif reference[kernel_name] != implementation[qemu_name]:
            errors.append(
                f"{qemu_name}=0x{implementation[qemu_name]:x}, "
                f"but pinned kernel says {kernel_name}=0x{reference[kernel_name]:x}"
            )
    dtsi = args.kernel_dtsi.read_text()
    node = re.search(
        r"uartblsp1dm1:\s*serial@0c170000\s*\{(.*?)\n\s*\};",
        dtsi, re.S
    )
    if not node:
        errors.append("pinned SoC DTSI is missing the uartblsp1dm1 node at 0x0c170000")
    else:
        reg = re.search(
            r"\breg\s*=\s*<\s*(0x[0-9a-fA-F]+|[0-9]+)\s+(0x[0-9a-fA-F]+|[0-9]+)\s*>",
            node.group(1)
        )
        if not reg:
            errors.append("uartblsp1dm1 node has no single-cell reg base/size tuple")
        else:
            base, window = (int(value, 0) for value in reg.groups())
            size_match = re.search(
                r"^\s*#define\s+UARTDM_MMIO_SIZE\s+(0x[0-9a-fA-F]+|[0-9]+)\b",
                args.qemu_source.read_text(), re.M
            )
            if base != 0x0c170000:
                errors.append(f"uartblsp1dm1 base is 0x{base:x}, expected 0xc170000")
            if size_match is None:
                errors.append("QEMU model is missing UARTDM_MMIO_SIZE")
            elif int(size_match.group(1), 0) != window:
                errors.append(
                    f"UARTDM_MMIO_SIZE=0x{int(size_match.group(1), 0):x}, "
                    f"but pinned DTSI declares window 0x{window:x}"
                )
    if errors:
        print("\n".join(errors))
        return 1
    print(
        f"validated {len(EXPECTED)} UARTDM register offsets, clear command, and MMIO window "
        "against pinned lavender kernel source"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
