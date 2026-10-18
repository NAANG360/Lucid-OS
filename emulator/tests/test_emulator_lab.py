import json, pathlib, subprocess, sys, tempfile, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
class EmulatorLabTests(unittest.TestCase):
    def test_profile_is_explicit_about_limits(self):
        p=json.loads((ROOT/"config/lavender-profile.json").read_text())
        self.assertEqual(p["target"]["codename"],"lavender")
        self.assertEqual(p["target"]["cpu_cores"],8)
        self.assertFalse(p["implementation_status"]["sdm660_specific_machine"])
        self.assertIn("not Kryo 260",p["generic_qemu_reference"]["warning"])
    def test_launcher_usage_is_safe(self):
        p=subprocess.run(["bash",str(ROOT/"run-generic-arm64.sh")],text=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        self.assertEqual(p.returncode,2)
        self.assertIn("Usage:",p.stderr)
    def test_launcher_requires_files(self):
        p=subprocess.run(["bash",str(ROOT/"run-generic-arm64.sh"),"/no/such/kernel","/no/such/initrd"],text=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        self.assertEqual(p.returncode,2)
        self.assertIn("Missing input",p.stderr)
if __name__=="__main__": unittest.main()
