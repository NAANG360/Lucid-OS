import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from inventory_dts_hardware import build_report, inventory, node_blocks, parse_cells

FIXTURE = r'''
/ {
  compatible = "qcom,sdm660";
  soc {
    #address-cells = <1>;
    #size-cells = <1>;
    intc: interrupt-controller@17a00000 {
      compatible = "arm,gic-v3";
      reg = <0x17a00000 0x10000>, <0x17b00000 0x100000>;
      interrupts = <1 9 4>;
    };
    uart0: serial@c1b000 {
      compatible = "qcom,geni-uart", "qcom,serial";
      reg = <0xc1b000 0x4000>;
      clock-names = "se-clk", "m-ahb";
      status = "okay";
    };
    memory@80000000 { device_type = "memory"; reg = <0x0 0x80000000 0x0 0x10000000>; };
  };
};
'''

class DTSInventoryTests(unittest.TestCase):
    def test_preserves_reg_tuples_without_guessing_address_cells(self):
        got = parse_cells("<0x17a00000 0x10000>, <0x17b00000 0x100000>")
        self.assertEqual(got, [[0x17a00000, 0x10000], [0x17b00000, 0x100000]])

    def test_nested_nodes_do_not_leak_properties_into_parent(self):
        nodes = list(node_blocks(FIXTURE))
        soc = next(n for n in nodes if n["node"] == "soc")
        self.assertNotIn("reg", soc["properties"])
        self.assertNotIn("interrupts", soc["properties"])

    def test_extracts_uart_and_interrupt_controller(self):
        report = build_report([("fixture.dts", FIXTURE)])
        self.assertEqual(report["target"]["soc"], "Qualcomm SDM660")
        self.assertEqual(report["counts"]["nodes_with_reg"], 3)
        self.assertEqual(report["boot_critical_candidates"]["interrupt_controller"][0]["reg_cells"][0][0], 0x17a00000)
        uart = report["boot_critical_candidates"]["uart"][0]
        self.assertEqual(uart["label"], "uart0")
        self.assertEqual(uart["clock_names"], ["se-clk", "m-ahb"])
        self.assertEqual(uart["status"], ["okay"])

    def test_report_discloses_preprocessing_limit(self):
        report = build_report([("fixture.dts", FIXTURE)])
        self.assertTrue(any("not a fully preprocessed" in s for s in report["limitations"]))

if __name__ == "__main__":
    unittest.main()
