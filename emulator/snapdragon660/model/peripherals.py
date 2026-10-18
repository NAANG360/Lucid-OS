"""Executable first-stage SDM660 peripheral model.

This is a deterministic register-level board model used to validate the guest-side
contract before wiring devices into QEMU. It is NOT a CPU emulator and does not
claim to model undocumented Qualcomm silicon behavior.
"""
from dataclasses import dataclass, field
from typing import Dict, List

# Register offsets and bit definitions cross-checked against public early-boot
# code for Qualcomm UARTDM. They describe the UARTDM transmit path only; this
# model is still not wired into QEMU's MMIO bus.
UARTDM_DMEN = 0x03C
UARTDM_SR = 0x0A4
UARTDM_CR = 0x0A8
UARTDM_TF = 0x100
UARTDM_TX_SINGLE_CHAR = 1 << 4
UARTDM_TX_BAM_ENABLE = 1 << 2
UARTDM_CR_RESET_TX = 2 << 4
UARTDM_CR_TX_ENABLE = 1 << 2
UARTDM_SR_TX_READY = 1 << 2

@dataclass
class QualcommUARTDMModel:
    """Small UARTDM transmit-register contract for early boot output."""
    tx: List[int] = field(default_factory=list)
    registers: Dict[int, int] = field(default_factory=lambda: {UARTDM_SR: UARTDM_SR_TX_READY})

    def read(self, offset: int) -> int:
        if offset == UARTDM_SR:
            return UARTDM_SR_TX_READY
        return self.registers.get(offset, 0)

    def write(self, offset: int, value: int) -> None:
        value &= 0xFFFFFFFF
        if offset == UARTDM_TF:
            # Firmware writes TF only when SR reports TX_READY.
            if self.read(UARTDM_SR) & UARTDM_SR_TX_READY:
                self.tx.append(value & 0xFF)
            return
        if offset == UARTDM_CR and value == UARTDM_CR_RESET_TX:
            self.tx.clear()
            self.registers[UARTDM_CR] = value
            return
        if offset == UARTDM_DMEN:
            # The early boot writer uses single-character FIFO mode and
            # disables BAM-based transmit for a simple polling console.
            value = (value & ~UARTDM_TX_BAM_ENABLE) | UARTDM_TX_SINGLE_CHAR
        self.registers[offset] = value

    def output(self) -> bytes:
        return bytes(self.tx)

@dataclass
class SDM660BoardModel:
    """Minimal observable board contract, not a silicon-complete emulator."""
    ram_size: int = 4 * 1024**3
    uart: QualcommUARTDMModel = field(default_factory=QualcommUARTDMModel)
    reset_reason: int = 0
    clock_state: Dict[str, bool] = field(default_factory=dict)
    reset_state: Dict[str, bool] = field(default_factory=dict)

    def reset(self, reason: int = 0) -> None:
        self.reset_reason = reason
        self.uart = QualcommUARTDMModel()
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
            self.uart.write(UARTDM_TF, byte)

    def snapshot(self) -> dict:
        return {
            "model": "sdm660-lavender-peripheral-contract-v1",
            "silicon_accurate": False,
            "bootable_android": False,
            "ram_size": self.ram_size,
            "reset_reason": self.reset_reason,
            "uart_tx_hex": self.uart.output().hex(),
            "clocks": dict(sorted(self.clock_state.items())),
            "resets": dict(sorted(self.reset_state.items())),
        }
