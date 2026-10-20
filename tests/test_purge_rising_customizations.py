import json
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "tools" / "purge_rising_customizations.py"

class PurgeRisingTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        for part in ("system", "product", "system_ext", "vendor", "odm"):
            (self.root / part).mkdir()
        self.aapt = self.root / "fake-aapt"
        self.aapt.write_text("""#!/bin/sh
case "$3" in
  *RisingClock.apk) echo "package: name='com.rising.clock'"; echo "application-label:'Rising Clock'";;
  *) echo "package: name='com.android.calculator'"; echo "application-label:'Calculator'";;
esac
""")
        self.aapt.chmod(0o755)
        self.report = self.root / "report.json"

    def tearDown(self):
        self.tmp.cleanup()

    def run_purge(self, *extra):
        return subprocess.run([
            "python3", str(SCRIPT), "--base-root", str(self.root),
            "--aapt", str(self.aapt), "--report", str(self.report), *extra
        ], text=True, capture_output=True)

    def test_removes_explicit_rising_app_and_properties_only(self):
        rising = self.root / "system/app/RisingClock/RisingClock.apk"
        rising.parent.mkdir(parents=True)
        rising.write_bytes(b"fake apk")
        calc = self.root / "system/app/Calculator/Calculator.apk"
        calc.parent.mkdir(parents=True)
        calc.write_bytes(b"fake apk")
        props = self.root / "system/build.prop"
        props.write_text("ro.rising.version=6.3\nro.build.version.sdk=35\n")
        result = self.run_purge("--apply")
        self.assertEqual(result.returncode, 0, result.stderr + result.stdout)
        self.assertFalse(rising.parent.exists())
        self.assertTrue(calc.exists())
        self.assertEqual(props.read_text(), "ro.build.version.sdk=35\n")
        report = json.loads(self.report.read_text())
        self.assertEqual(len(report["removed"]), 1)
        self.assertEqual(report["removed"][0]["package"], "com.rising.clock")
        self.assertEqual(len(report["branding_edits"]), 1)

    def test_unreviewed_rising_configuration_blocks_packaging(self):
        p = self.root / "system/etc/permissions/rising-permissions.xml"
        p.parent.mkdir(parents=True)
        p.write_text("<permissions/>")
        result = self.run_purge("--apply")
        self.assertEqual(result.returncode, 1)
        self.assertTrue(p.exists())
        report = json.loads(self.report.read_text())
        self.assertTrue(any("requires review" in item for item in report["unresolved"]))

if __name__ == "__main__":
    unittest.main()
