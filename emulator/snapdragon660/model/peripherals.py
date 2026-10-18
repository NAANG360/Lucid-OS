"""Executable first-stage SDM660 peripheral model.

This is a deterministic register-level board model used to validate the guest-side
contract before wiring devices into QEMU. It is NOT a CPU emulator and does not
claim to model undocumented Qualcomm silicon behavior.
"""
from dataclasses import dataclass, field
from typing import Dict, List

# Public Qualcomm device trees describe these blocks by compatible string, but
# addresses/bitfields must come from matching public kernel drivers or docs.
# Therefore this model uses symbolic offsets, never fabricated physical MMIO.
UART_DR = 0x00
UART_FR = 0x18
UART_FR_TXFF = 1 << 5
UART_FR_TXFE = 1 << 7
UART_CR = 0x30
UART_LCRH = 0x2C
UART_IBRD = 0x24
UART_FBRD = 0x28
UART_IMSC = 0x38
UART_ICR = 0x44

@dataclass
class PL011Model:
    """PL011-compatible console behavior for early boot diagnostics."""
    tx: List[int] = field(default_factory=list)
    registers: Dict[int, int] = field(default_factory=dict)

    def read(self, offset: int) -> int:
        if offset == UART_FR:
            return UART_FR_TXFE
        if offset == UART_DR:
            return 0
        return self.registers.get(offset, 0)

    def write(self, offset: int, value: int) -> None:
        value &= 0xFFFFFFFF
        if offset == UART_DR:
            self.tx.append(value & 0xFF)
            return
        if offset == UART_ICR:
            # Interrupt clear is write-one-to-clear; no interrupt source is
            # asserted by this first-stage console-only model.
            self.registers[UART_ICR] = value
            return
        self.registers[offset] = value

    def output(self) -> bytes:
        return bytes(self.tx)

@dataclass
class SDM660BoardModel:
    """Minimal observable board contract, not a silicon-complete emulator."""
    ram_size: int = 4 * 1024**3
    uart: PL011Model = field(default_factory=PL011Model)
    reset_reason: int = 0
    clock_state: Dict[str, bool] = field(default_factory=dict)
    reset_state: Dict[str, bool] = field(default_factory=dict)

    def reset(self, reason: int = 0) -> None:
        self.reset_reason = reason
        self.uart = PL011Model()
        self.clock_state.clear()
        self.reset_state.clear()

    def set_clock(self, name: str, enabled: bool) -> None:
        if not name or not isinstance(enabled, bool):
            raise ValueError("clock name must be nonempty and enabled must be bool")
        self.clock_state[name] = enabled

    def set_reset(self, name: str, asserted: bool) -> None:
        if not name or not isinstance(asserted, bool):
            raise ValueError("reset name must be nonempty and asserted must be bool")
        self.reset_state[name] = asserted

    def guest_console_write(self, data: bytes) -> None:
        for byte in data:
            self.uart.write(UART_DR, byte)

    def snapshot(self) -> dict:
        return {
            "model": "sdm660-lavender-peripheral-contract-v0",
            "silicon_accurate": False,
            "bootable_android": False,
            "ram_size": self.ram_size,
            "reset_reason": self.reset_reason,
            "uart_tx_hex": self.uart.output().hex(),
            "clocks": dict(sorted(self.clock_state.items())),
            "resets": dict(sorted(self.reset_state.items())),
        }
