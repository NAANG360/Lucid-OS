#!/usr/bin/env python3
"""Cross-check the native lavender machine's first UART/GIC map against pinned SoC DTSI."""
from __future__ import annotations

import argparse
import re
from pathlib import Path


def define_int(source: str, name: str) -> int:
    match = re.search(
        rf"^\s*#define\s+{re.escape(name)}\s+(0x[0-9a-fA-F]+|[0-9]+)(?:ULL|UL|U|L)?\b",
        source, re.M,
    )
    if not match:
        raise ValueError(f"machine source missing numeric define {name}")
    return int(match.group(1), 0)


def validate(dtsi: str, machine: str) -> dict:
    node_match = re.search(
        r"uartblsp1dm1:\s*serial@0c170000\s*\{(.*?)\n\s*\};",
        dtsi, re.S,
    )
    if not node_match:
        raise ValueError("pinned SDM660 DTSI has no uartblsp1dm1 node at 0x0c170000")
    node = node_match.group(1)

    compatible_match = re.search(r'\bcompatible\s*=\s*((?:"[^"]+"\s*,?\s*)+);', node)
    if not compatible_match:
        raise ValueError("uartblsp1dm1 has no compatible string list")
    compatibles = re.findall(r'"([^"]+)"', compatible_match.group(1))
    if not any(value in machine for value in compatibles):
        raise ValueError(
            "native machine UART compatible does not match pinned DTSI: "
            + ", ".join(compatibles)
        )

    reg_match = re.search(
        r"\breg\s*=\s*<\s*(0x[0-9a-fA-F]+|[0-9]+)\s+"
        r"(0x[0-9a-fA-F]+|[0-9]+)\s*>", node,
    )
    if not reg_match:
        raise ValueError("uartblsp1dm1 reg is not a simple base/size tuple")
    base, size = (int(value, 0) for value in reg_match.groups())

    irq_match = re.search(
        r"\binterrupts\s*=\s*<\s*(0x[0-9a-fA-F]+|[0-9]+)\s+"
        r"(0x[0-9a-fA-F]+|[0-9]+)\s+"
        r"(0x[0-9a-fA-F]+|[0-9]+)\s*>", node,
    )
    if not irq_match:
        raise ValueError("uartblsp1dm1 interrupts is not a simple three-cell tuple")
    irq_type, irq_number, irq_flags = (int(value, 0) for value in irq_match.groups())

    expected = {
        "SDM660_UART_BASE": base,
        "SDM660_UART_SIZE": size,
        "SDM660_UART_SPI": irq_number,
    }
    for name, value in expected.items():
        actual = define_int(machine, name)
        if actual != value:
            raise ValueError(f"{name}=0x{actual:x}, pinned DTSI requires 0x{value:x}")

    if irq_type != 0:
        raise ValueError(f"UART interrupt type cell {irq_type} is not an SPI (0)")
    uart_fdt_match = re.search(
        r'qemu_fdt_add_subnode\(fdt, "/soc/serial@c170000"\);(.*?)(?=qemu_fdt_add_subnode\(fdt, "/aliases"\);)',
        machine, re.S,
    )
    if not uart_fdt_match:
        raise ValueError("native machine does not construct the UART FDT node")
    uart_fdt = uart_fdt_match.group(1)
    if '"interrupt-parent"' not in uart_fdt or '"interrupts"' not in uart_fdt:
        raise ValueError("UART FDT node is missing its interrupt-parent/interrupts properties")
    if not re.search(r'"interrupt-parent",\s*1', uart_fdt):
        raise ValueError("UART FDT interrupt-parent does not reference the native GIC phandle")
    if 'qemu_fdt_setprop_cell(fdt, "/", "interrupt-parent", 1)' not in machine:
        raise ValueError("root FDT interrupt-parent does not reference the native GIC phandle")
    if f"0, SDM660_UART_SPI, {irq_flags}" not in uart_fdt:
        raise ValueError(
            f"UART FDT interrupt tuple must preserve DTSI cells <{irq_type} {irq_number} {irq_flags}>"
        )
    if "32 + SDM660_UART_SPI" not in machine:
        raise ValueError("UART SPI must connect to GIC INTID 32 + SPI number")

    timer_match = re.search(r"\btimer\s*\{(.*?)\n\s*\};", dtsi, re.S)
    if not timer_match:
        raise ValueError("pinned SDM660 DTSI has no architectural timer node")
    timer_interrupts = re.search(r"\binterrupts\s*=\s*(.*?);", timer_match.group(1), re.S)
    if not timer_interrupts:
        raise ValueError("pinned SDM660 timer node has no interrupts property")
    timer_tuples = [
        tuple(int(value, 0) for value in values)
        for values in re.findall(
            r"<\s*(0x[0-9a-fA-F]+|[0-9]+)\s+"
            r"(0x[0-9a-fA-F]+|[0-9]+)\s+"
            r"(0x[0-9a-fA-F]+|[0-9]+)\s*>",
            timer_interrupts.group(1),
        )
    ]
    if len(timer_tuples) != 4:
        raise ValueError(f"expected four SDM660 architectural timer PPIs, found {timer_tuples}")
    timer_fdt_match = re.search(
        r'qemu_fdt_add_subnode\(fdt, "/timer"\);(.*?)'
        r'(?=qemu_fdt_add_subnode\(fdt, "/intc"\);)',
        machine, re.S,
    )
    if not timer_fdt_match:
        raise ValueError("native machine does not construct the architectural timer node")
    timer_fdt = timer_fdt_match.group(1)
    timer_fdt_irqs = re.search(
        r'qemu_fdt_setprop_cells\(fdt, "/timer", "interrupts",\s*(.*?)\);',
        timer_fdt, re.S,
    )
    if not timer_fdt_irqs:
        raise ValueError("native machine timer node has no interrupts property")
    timer_values = [
        int(value, 0)
        for value in re.findall(r"0x[0-9a-fA-F]+|[0-9]+", timer_fdt_irqs.group(1))
    ]
    expected_timer_values = [value for row in timer_tuples for value in row]
    if timer_values != expected_timer_values:
        raise ValueError(
            f"timer FDT cells {timer_values} do not match pinned DTSI {expected_timer_values}"
        )
    for cpu_output, ppi in zip(
        ("GTIMER_PHYS", "GTIMER_VIRT", "GTIMER_HYP", "GTIMER_SEC"),
        (row[1] for row in timer_tuples),
    ):
        if not re.search(rf"\[{cpu_output}\]\s*=\s*{ppi}\b", machine):
            raise ValueError(f"{cpu_output} must be connected to SDM660 timer PPI {ppi}")

    gic_match = re.search(
        r"intc:\s*interrupt-controller@17a00000\s*\{(.*?)\n\s*\};",
        dtsi, re.S,
    )
    if not gic_match:
        raise ValueError("pinned SoC DTSI has no GICv3 node at 0x17a00000")
    gic_node = gic_match.group(1)
    if '"arm,gic-v3"' not in gic_node or '"arm,gic-v3"' not in machine:
        raise ValueError("native machine and pinned DTSI must both identify GICv3")
    gic_reg = re.search(r"\breg\s*=\s*(.*?);", gic_node, re.S)
    if not gic_reg:
        raise ValueError("GICv3 node has no reg property")
    stride_match = re.search(
        r"\bredistributor-stride\s*=\s*<\s*(0x[0-9a-fA-F]+|[0-9]+)\s+"
        r"(0x[0-9a-fA-F]+|[0-9]+)\s*>",
        gic_node,
    )
    if not stride_match:
        raise ValueError("pinned GICv3 node has no two-cell redistributor-stride")
    stride_cells = tuple(int(value, 0) for value in stride_match.groups())
    expected_stride = (stride_cells[0] << 32) | stride_cells[1]
    machine_stride = re.search(
        r'"redistributor-stride",\s*2,\s*(0x[0-9a-fA-F]+|[0-9]+)',
        machine,
    )
    if not machine_stride or int(machine_stride.group(1), 0) != expected_stride:
        raise ValueError(
            f"machine GIC redistributor stride does not match DTSI 0x{expected_stride:x}"
        )
    gic_interrupts = re.search(
        r"\binterrupts\s*=\s*<\s*(0x[0-9a-fA-F]+|[0-9]+)\s+"
        r"(0x[0-9a-fA-F]+|[0-9]+)\s+"
        r"(0x[0-9a-fA-F]+|[0-9]+)\s*>",
        gic_node,
    )
    if not gic_interrupts:
        raise ValueError("pinned GICv3 node has no maintenance interrupt tuple")
    maintenance_cells = tuple(int(value, 0) for value in gic_interrupts.groups())
    if define_int(machine, "SDM660_GIC_MAINT_IRQ") != maintenance_cells[1]:
        raise ValueError("native GICv3 maintenance PPI does not match pinned DTSI")
    if not re.search(
        r' qemu_fdt_setprop_cells\(fdt, "/intc", "interrupts",\s*'
        r'1,\s*SDM660_GIC_MAINT_IRQ,\s*4\)',
        machine,
    ):
        raise ValueError("native GICv3 FDT node does not describe its maintenance PPI")
    gic_cells = [
        int(value, 0)
        for value in re.findall(r"0x[0-9a-fA-F]+|[0-9]+", gic_reg.group(1))
    ]
    if len(gic_cells) < 4:
        raise ValueError("GICv3 reg property does not contain distributor and redistributor tuples")
    gic_expected = {
        "SDM660_GICD_BASE": gic_cells[0],
        "SDM660_GICD_SIZE": gic_cells[1],
        "SDM660_GICR_BASE": gic_cells[2],
        "SDM660_GICR_SIZE": gic_cells[3],
    }
    for name, value in gic_expected.items():
        actual = define_int(machine, name)
        if actual != value:
            raise ValueError(f"{name}=0x{actual:x}, pinned DTSI requires 0x{value:x}")

    return {
        "gic_reg_cells": [hex(value) for value in gic_cells[:4]],
        "timer_interrupt_cells": [[hex(value) for value in row] for row in timer_tuples],
        "compatible": compatibles,
        "mmio_base": hex(base),
        "mmio_size": hex(size),
        "interrupt_cells": [irq_type, irq_number, irq_flags],
        "status": "source-contract-matched; not proof of retail DT enablement",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--kernel-dtsi", required=True, type=Path)
    parser.add_argument("--machine-source", required=True, type=Path)
    args = parser.parse_args()
    try:
        report = validate(
            args.kernel_dtsi.read_text(encoding="utf-8"),
            args.machine_source.read_text(encoding="utf-8"),
        )
    except (OSError, ValueError) as exc:
        print(f"FAIL: {exc}")
        return 1
    import json
    print(json.dumps(report, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
