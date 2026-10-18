import importlib.util
import tempfile
import unittest
import zipfile
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1] / "tools" / "inspect_firmware_bundle.py"
spec = importlib.util.spec_from_file_location("inspect_firmware_bundle", MODULE)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class FirmwareInventoryTests(unittest.TestCase):
    def test_android_signatures(self):
        self.assertIn("android-boot-image", module.classify_header(b"ANDROID!" + b"\0" * 32, 4096))
        self.assertIn("android-vbmeta", module.classify_header(b"AVB0" + b"\0" * 32, 4096))
        self.assertIn("android-sparse-image", module.classify_header(bytes.fromhex("3aff26ed") + b"\0" * 32, 4096))

    def test_gpt_detected_at_lba_one(self):
        header = bytearray(4096)
        header[512:520] = b"EFI PART"
        with tempfile.TemporaryDirectory() as td:
            p = Path(td) / "disk.img"
            p.write_bytes(header)
            result = module.inspect_file(p)
        self.assertIn("gpt-header-at-lba1", result["classifications"])

    def test_archive_is_inventoried_without_extraction(self):
        with tempfile.TemporaryDirectory() as td:
            p = Path(td) / "service.zip"
            with zipfile.ZipFile(p, "w") as z:
                z.writestr("boot.img", b"ANDROID!" + b"\0" * 32)
            result = module.inspect_archive(p)
        self.assertEqual(result[0]["path"], "boot.img")
        self.assertIn("android-boot-image", result[0]["classifications"])
        self.assertIn("not fully hashed", result[0]["note"])


if __name__ == "__main__":
    unittest.main()
