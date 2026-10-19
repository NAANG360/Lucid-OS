import importlib.util
import tempfile
import unittest
from pathlib import Path

TOOL = Path(__file__).resolve().parents[1] / "tools" / "validate_uartdm_register_map.py"
SPEC = importlib.util.spec_from_file_location("validate_uartdm_register_map", TOOL)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class UARTDMRegisterMapTests(unittest.TestCase):
    def test_parser_reads_kernel_enum_assignments(self):
        with tempfile.TemporaryDirectory() as temp:
            header = Path(temp) / "msm_serial_hs_hwreg.h"
            header.write_text(
                "enum msm_hs_regs {\n"
                "    UART_DM_SR = 0xa4,\n"
                "    UART_DM_CR = 0xa8,\n"
                "    UART_DM_DMEN = 0x3c,\n"
                "    UART_DM_ISR = 0xb4,\n"
                "    UART_DM_MISR = 0xac,\n"
                "#define CLEAR_TX_READY 0x300\n"
                "};\n"
                "#define UARTDM_ISR_TX_READY_BMSK BIT(7)\n"
            )
            values = MODULE.defines(header)
            self.assertEqual(values["UART_DM_SR"], 0xA4)
            self.assertEqual(values["UART_DM_CR"], 0xA8)
            self.assertEqual(values["UART_DM_DMEN"], 0x3C)
            self.assertEqual(values["UARTDM_ISR_TX_READY_BMSK"], 0x80)

    def test_parser_reads_qemu_preprocessor_constants(self):
        with tempfile.TemporaryDirectory() as temp:
            source = Path(temp) / "sdm660_uartdm.c"
            source.write_text(
                "#define UARTDM_DMEN 0x03c\n"
                "#define UARTDM_SR 0x0a4\n"
                "#define UARTDM_CR 0x0a8\n"
                "#define UARTDM_ISR 0x0b4\n"
                "#define UARTDM_MISR 0x0ac\n"
                "#define UARTDM_CR_CLEAR_TX_READY 0x300\n"
            )
            values = MODULE.defines(source)
            self.assertEqual(values["UARTDM_DMEN"], 0x3C)
            self.assertEqual(values["UARTDM_SR"], 0xA4)
            self.assertEqual(values["UARTDM_CR"], 0xA8)


if __name__ == "__main__":
    unittest.main()
