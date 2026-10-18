import json, subprocess, sys, unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]

class SnapdragonVariantTests(unittest.TestCase):
    def test_profile_is_explicitly_snapdragon(self):
        p=json.loads((ROOT/"profile.json").read_text())
        self.assertEqual(p["soc"]["part"], "SDM660")
        self.assertEqual(p["device"]["codename"], "lavender")
        self.assertEqual(p["implementation"]["qemu_machine_type"], "sdm660-lavender")
        self.assertFalse(p["implementation"]["real_sdm660_machine_model"])
    def test_no_debian_guest_boot_is_part_of_this_variant(self):
        wf=(ROOT.parents[1]/".github/workflows/snapdragon660-variant.yml").read_text()
        self.assertNotIn("Debian", wf)
        self.assertIn("Snapdragon 660 Variant", wf)
    def test_platform_description_generation(self):
        p=subprocess.run([sys.executable, str(ROOT/"tools/build_platform_description.py")], capture_output=True, text=True)
        self.assertEqual(p.returncode, 0, p.stderr)
        d=json.loads(p.stdout)
        self.assertEqual(d["qemu_machine_type"], "sdm660-lavender")
        self.assertEqual(d["device_tree_scaffold_status"], "disabled-incomplete-not-bootable")
    def test_dts_does_not_claim_verified_silicon_map(self):
        dts=(ROOT/"device-tree/sdm660-lavender.dts").read_text()
        self.assertIn("does not invent Qualcomm MMIO addresses", dts)
        self.assertIn('compatible = "xiaomi,lavender-sdm660", "qcom,sdm660";', dts)

if __name__=="__main__": unittest.main()
