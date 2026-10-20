#!/usr/bin/env python3
"""Regression tests for ordered transfer-list reconstruction commands."""
import os
import subprocess
import struct
import sys
import tempfile
import unittest

import brotli

SCRIPT = os.path.join(os.path.dirname(os.path.dirname(__file__)),
                      "tools", "reconstruct_transfer_image.py")
BLOCK = 4096


class TransferImageReconstructionTests(unittest.TestCase):
    def test_zero_command_clears_previously_written_overlapping_range(self):
        with tempfile.TemporaryDirectory() as temp:
            transfer = os.path.join(temp, "product.transfer.list")
            compressed = os.path.join(temp, "product.new.dat.br")
            output = os.path.join(temp, "product.img")

            # Write blocks 0-1, clear block 1, then write block 2.
            # new.dat contains data only for the two "new" commands.
            with open(transfer, "w", encoding="utf-8") as stream:
                stream.write("4\n4\n0\n0\n"
                             "new 2,0,2\n"
                             "zero 2,1,2\n"
                             "new 2,2,3\n")
            first_block = bytearray(b"A" * BLOCK)
            # Minimal ext4 superblock fields at file offset 1024 so the test
            # exercises unique-coverage accounting with overlapping ranges.
            struct.pack_into("<I", first_block, 1028, 12)  # 12 ext4 blocks at 1 KiB each
            struct.pack_into("<I", first_block, 1048, 0)  # s_log_block_size => 1024
            first_block[1080:1082] = b"\x53\xef"
            first_block[1360:1364] = bytes(4)  # s_blocks_count_hi
            raw = bytes(first_block) + (b"B" * BLOCK) + (b"C" * BLOCK)
            with open(compressed, "wb") as stream:
                stream.write(brotli.compress(raw))

            result = subprocess.run(
                [sys.executable, SCRIPT, transfer, compressed, output],
                check=True, capture_output=True, text=True,
            )
            self.assertIn("new_plus_zero_unique_block_coverage=3/3 (100.00%)", result.stdout)
            self.assertIn("new_zero_plus_erase_unique_block_coverage=3/3 (100.00%)", result.stdout)
            self.assertIn("transfer_covers_full_filesystem_by_operations=True", result.stdout)

            with open(output, "rb") as stream:
                image = stream.read()
            self.assertEqual(len(image), 3 * BLOCK)
            self.assertEqual(image[:BLOCK], bytes(first_block))
            self.assertEqual(image[BLOCK:2 * BLOCK], bytes(BLOCK))
            self.assertEqual(image[2 * BLOCK:], b"C" * BLOCK)

    def test_erase_does_not_count_as_known_filesystem_content(self):
        with tempfile.TemporaryDirectory() as temp:
            transfer = os.path.join(temp, "product.transfer.list")
            compressed = os.path.join(temp, "product.new.dat.br")
            output = os.path.join(temp, "product.img")

            # The final block is erased/discarded, not populated by a known
            # new/zero write. A blank-image reconstruction cannot claim the
            # original filesystem is fully represented just because it has
            # an erase operation for that range.
            with open(transfer, "w", encoding="utf-8") as stream:
                stream.write("4\\n2\\n0\\n0\\n"
                             "new 2,0,2\\n"
                             "erase 2,2,3\\n")
            first_block = bytearray(b"A" * BLOCK)
            struct.pack_into("<I", first_block, 1028, 12)
            struct.pack_into("<I", first_block, 1048, 0)
            first_block[1080:1082] = b"\\x53\\xef"
            first_block[1360:1364] = bytes(4)
            with open(compressed, "wb") as stream:
                stream.write(brotli.compress(bytes(first_block) + b"B" * BLOCK))

            result = subprocess.run(
                [sys.executable, SCRIPT, transfer, compressed, output],
                check=True, capture_output=True, text=True,
            )
            self.assertIn("new_plus_zero_unique_block_coverage=2/3 (66.67%)", result.stdout)
            self.assertIn("new_zero_plus_erase_unique_block_coverage=3/3 (100.00%)", result.stdout)
            self.assertIn("transfer_covers_full_filesystem_by_operations=False", result.stdout)
            self.assertIn("does not write every declared filesystem block", result.stdout)

    def test_rejects_empty_range_fields(self):
        with tempfile.TemporaryDirectory() as temp:
            transfer = os.path.join(temp, "product.transfer.list")
            compressed = os.path.join(temp, "product.new.dat.br")
            output = os.path.join(temp, "product.img")
            with open(transfer, "w", encoding="utf-8") as stream:
                stream.write("4\n1\n0\n0\nnew 2,0,,1\n")
            with open(compressed, "wb") as stream:
                stream.write(b"not-needed")
            result = subprocess.run(
                [sys.executable, SCRIPT, transfer, compressed, output],
                capture_output=True, text=True,
            )
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("invalid range command", result.stderr)


if __name__ == "__main__":
    unittest.main()
