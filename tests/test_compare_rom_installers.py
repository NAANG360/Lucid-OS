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
            "4\n7\n0\n0\n"
            "new 4,0,3,10,12\n"
            "zero 2,3,5\n"
            "erase 2,20,22\n"
        ).encode()
        result = "\n".join(MODULE.transfer_summary(data))
        self.assertIn("command_counts={'new': 1, 'zero': 1, 'erase': 1}", result)
        self.assertIn("range_blocks_by_command={'new': 5, 'zero': 2, 'erase': 2}", result)
        self.assertIn("malformed_range_commands=0", result)
        self.assertIn("write_block_count_matches_header=True", result)

    def test_rejects_malformed_range_count(self):
        data = b"4\n1\n0\n0\nnew 4,0,3\n"
        result = "\n".join(MODULE.transfer_summary(data))
        self.assertIn("malformed_range_commands=1", result)

    def test_rejects_negative_or_reversed_ranges(self):
        data = b"4\n1\n0\n0\nnew 4,-1,2,8,7\n"
        result = "\n".join(MODULE.transfer_summary(data))
        self.assertIn("malformed_range_commands=1", result)
        self.assertIn("range_blocks_by_command={}", result)

    def test_operation_specific_commands_are_counted_not_misparsed(self):
        data = (
            "4\n2\n0\n0\n"
            "new 2,0,2\n"
            "move 4,0,10,20,30\n"
            "stash 1,sha256-example\n"
            "free 1\n"
            "bsdiff 0 1 2 3 4\n"
        ).encode()
        result = "\n".join(MODULE.transfer_summary(data))
        self.assertIn("'move': 1", result)
        self.assertIn("'stash': 1", result)
        self.assertIn("'free': 1", result)
        self.assertIn("'bsdiff': 1", result)
        self.assertIn("malformed_range_commands=0", result)
        self.assertIn("operation_specific_commands_not_interpreted=", result)

    def test_reports_write_count_mismatch(self):
        data = b"4\n9\n0\n0\nnew 2,0,2\n"
        result = "\n".join(MODULE.transfer_summary(data))
        self.assertIn("expected_write_blocks=9", result)
        self.assertIn("counted_new_plus_zero_blocks=2", result)
        self.assertIn("write_block_count_matches_header=False", result)


if __name__ == "__main__":
    unittest.main()
