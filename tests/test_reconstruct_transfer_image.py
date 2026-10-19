#!/usr/bin/env python3
"""Regression tests for ordered transfer-list reconstruction commands."""
import os
import subprocess
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
            raw = (b"A" * BLOCK) + (b"B" * BLOCK) + (b"C" * BLOCK)
            with open(compressed, "wb") as stream:
                stream.write(brotli.compress(raw))

            subprocess.run(
                [sys.executable, SCRIPT, transfer, compressed, output],
                check=True, capture_output=True, text=True,
            )

            with open(output, "rb") as stream:
                image = stream.read()
            self.assertEqual(len(image), 3 * BLOCK)
            self.assertEqual(image[:BLOCK], b"A" * BLOCK)
            self.assertEqual(image[BLOCK:2 * BLOCK], bytes(BLOCK))
            self.assertEqual(image[2 * BLOCK:], b"C" * BLOCK)


if __name__ == "__main__":
    unittest.main()
