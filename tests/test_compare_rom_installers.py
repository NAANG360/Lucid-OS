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

    def test_operation_specific_commands_are_counted_and_target_ranges_parsed(self):
        data = (
            "4\n6\n0\n0\n"
            "new 2,0,2\n"
            "move sha256-source 2,0,2 2 2,4,6\n"
            "stash stash-id 2,4,6\n"
            "free stash-id\n"
            "bsdiff 0 10 sha256-source sha256-target 2,2,4 2 2,4,6\n"
        ).encode()
        result = "\n".join(MODULE.transfer_summary(data))
        self.assertIn("'move': 1", result)
        self.assertIn("'stash': 1", result)
        self.assertIn("'free': 1", result)
        self.assertIn("'bsdiff': 1", result)
        self.assertIn("target_write_blocks_by_command={'new': 2, 'move': 2, 'bsdiff': 2}", result)
        self.assertIn("malformed_target_range_commands=0", result)
        self.assertIn("write_block_count_matches_header=True", result)

    def test_reports_write_count_mismatch(self):
        data = b"4\n9\n0\n0\nnew 2,0,2\n"
        result = "\n".join(MODULE.transfer_summary(data))
        self.assertIn("expected_write_blocks=9", result)
        self.assertIn("counted_target_write_blocks=2", result)
        self.assertIn("write_block_count_matches_header=False", result)

    def test_rejects_malformed_diff_target_range(self):
        data = b"4\n0\n0\n0\nbsdiff 0 10 source target 3,0,1 1 1,2\n"
        result = "\n".join(MODULE.transfer_summary(data))
        self.assertIn("malformed_target_range_commands=1", result)


if __name__ == "__main__":
    unittest.main()
