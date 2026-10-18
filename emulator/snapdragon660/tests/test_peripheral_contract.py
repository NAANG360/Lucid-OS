import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "model"))
from peripherals import (
    SDM660BoardModel, QualcommUARTDMModel, UARTDM_DMEN, UARTDM_SR,
    UARTDM_SR_TX_READY, UARTDM_TF, UARTDM_TX_BAM_ENABLE,
    UARTDM_TX_SINGLE_CHAR, UARTDM_CR, UARTDM_CR_RESET_TX,
)

class PeripheralContractTests(unittest.TestCase):
    def test_uartdm_console_transmit_round_trip(self):
        board = SDM660BoardModel()
        board.guest_console_write(b"SDM660 lavender boot\\r\\n")
        self.assertEqual(board.uart.output(), b"SDM660 lavender boot\\r\\n")
        self.assertEqual(board.uart.read(UARTDM_SR) & UARTDM_SR_TX_READY, UARTDM_SR_TX_READY)

    def test_uartdm_transmit_fifo_emits_low_byte(self):
        uart = QualcommUARTDMModel()
        uart.write(UARTDM_TF, 0x1234)
        self.assertEqual(uart.output(), b"4")

    def test_uartdm_dmen_selects_polled_single_character_mode(self):
        uart = QualcommUARTDMModel()
        uart.write(UARTDM_DMEN, 0xFFFFFFFF)
        self.assertTrue(uart.read(UARTDM_DMEN) & UARTDM_TX_SINGLE_CHAR)
        self.assertFalse(uart.read(UARTDM_DMEN) & UARTDM_TX_BAM_ENABLE)

    def test_uartdm_transmit_reset_clears_buffer(self):
        uart = QualcommUARTDMModel()
        uart.write(UARTDM_TF, ord("x"))
        uart.write(UARTDM_CR, UARTDM_CR_RESET_TX)
        self.assertEqual(uart.output(), b"")

    def test_clock_and_reset_state_are_independent(self):
        board = SDM660BoardModel()
        board.set_clock("gcc.uart", True)
        board.set_reset("uart", False)
        self.assertTrue(board.clock_state["gcc.uart"])
        self.assertFalse(board.reset_state["uart"])

    def test_board_reset_clears_console_and_control_state(self):
        board = SDM660BoardModel()
        board.guest_console_write(b"old")
        board.set_clock("gcc.uart", True)
        board.reset(reason=7)
        self.assertEqual(board.reset_reason, 7)
        self.assertEqual(board.uart.output(), b"")
        self.assertEqual(board.clock_state, {})

    def test_snapshot_does_not_claim_full_emulation(self):
        snapshot = SDM660BoardModel().snapshot()
        self.assertFalse(snapshot["silicon_accurate"])
        self.assertFalse(snapshot["bootable_android"])
        self.assertEqual(snapshot["ram_size"], 4 * 1024**3)

    def test_invalid_clock_contract_is_rejected(self):
        with self.assertRaises(ValueError):
            SDM660BoardModel().set_clock("", True)

if __name__ == "__main__":
    unittest.main()
