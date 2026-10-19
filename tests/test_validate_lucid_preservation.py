#!/usr/bin/env python3
"""Unit tests for LucidOS preservation manifest generation."""
import importlib.util
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).resolve().parents[1] / "tools" / "validate_lucid_preservation.py"
SPEC = importlib.util.spec_from_file_location("validate_lucid_preservation", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(MODULE)

class PreservationManifestTests(unittest.TestCase):
    def test_reports_missing_required_paths_and_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            result = MODULE.build_manifest(Path(tmp))
            self.assertFalse(result["pass"])
            self.assertEqual(result["present_required_count"], 0)
            self.assertEqual(set(result["missing_required"]), set(MODULE.REQUIRED))

    def test_hashes_required_files_and_detects_init_lucid_rd(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for rel in MODULE.REQUIRED:
                path = root / rel
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(("test:" + rel).encode())
            ramdisk = root / "boot-ramdisk/etc/init.lucid.rd"
            ramdisk.parent.mkdir(parents=True)
            ramdisk.write_text("service lucid_test")
            result = MODULE.build_manifest(root)
            self.assertTrue(result["pass"])
            self.assertEqual(result["present_required_count"], len(MODULE.REQUIRED))
            self.assertTrue(result["init_file_search"]["init_lucid_rd_found"])
            self.assertTrue(all(len(item["sha256"]) == 64 for item in result["required_files"]))

if __name__ == "__main__":
    unittest.main()
