#!/usr/bin/env python3
"""Unit tests for ROM installer comparison helpers."""
import importlib.util
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).resolve().parents[1] / "tools" / "compare_rom_installers.py"
SPEC = importlib.util.spec_from_file_location("compare_rom_installers", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(MODULE)

class TransferSummaryTests(unittest.TestCase):
    def test_counts_ranges_by_operation_and_validates_ranges(self):
        data = (
            "4\n10\n0\n0\n"
            "new 4,0,3,10,12\n"
            "zero 2,3,5\n"
            "erase 2,20,22\n"
        ).encode()
        result = "\n".join(MODULE.transfer_summary(data))
        self.assertIn("command_counts={'new': 1, 'zero': 1, 'erase': 1}", result)
        self.assertIn("block_counts_by_command={'new': 5, 'zero': 2, 'erase': 2}", result)
        self.assertIn("malformed_commands=0", result)

    def test_rejects_malformed_range_count(self):
        data = b"4\n1\n0\n0\nnew 4,0,3\n"
        result = "\n".join(MODULE.transfer_summary(data))
        self.assertIn("malformed_commands=1", result)

    def test_rejects_negative_or_reversed_ranges(self):
        data = b"4\n1\n0\n0\nnew 4,-1,2,8,7\n"
        result = "\n".join(MODULE.transfer_summary(data))
        self.assertIn("malformed_commands=2", result)
        self.assertIn("block_counts_by_command={}", result)

if __name__ == "__main__":
    unittest.main()
