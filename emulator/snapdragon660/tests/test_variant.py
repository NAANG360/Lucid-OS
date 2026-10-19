import json, subprocess, sys, unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]

class SnapdragonVariantTests(unittest.TestCase):
    def test_profile_is_explicitly_snapdragon(self):
        p=json.loads((ROOT/"profile.json").read_text())
        self.assertEqual(p["soc"]["part"], "SDM660")
        self.assertEqual(p["device"]["codename"], "lavender")
        self.assertEqual(p["implementation"]["qemu_machine_type"], "sdm660-lavender")
        self.assertTrue(p["implementation"]["real_sdm660_machine_model"])
        machine = ROOT / "qemu" / "hw" / "arm" / "sdm660_lavender.c"
        self.assertTrue(machine.is_file())
        source = machine.read_text()
        self.assertIn('MACHINE_TYPE_NAME("sdm660-lavender")', source)
        self.assertIn("SDM660_GICD_BASE", source)
        self.assertIn("SDM660_UART_BASE", source)
        self.assertIn("lavender_mpidr", source)
        self.assertIn("CPU(obj)->cpu_index = i", source)
        self.assertIn("gicv3_class_name()", source)
        self.assertIn("arm_load_kernel", source)
        self.assertNotIn("TYPE_VIRT_MACHINE", source)
    def test_uart_fdt_exposes_its_own_interrupt_tuple(self):
        source = (ROOT / "qemu" / "hw" / "arm" / "sdm660_lavender.c").read_text()
        uart = source.split('qemu_fdt_add_subnode(fdt, "/soc/serial@c170000");', 1)[1].split('qemu_fdt_add_subnode(fdt, "/aliases");', 1)[0]
        self.assertIn('"interrupt-parent"', uart)
        self.assertIn('"interrupts"', uart)
        self.assertIn("0, SDM660_UART_SPI, 0", uart)
        # Timer PPIs legitimately use 0xf08; they must not satisfy the UART check.
        self.assertNotIn("0, SDM660_UART_SPI, 0xf08", uart)

    def test_cpu_topology_is_eight_unique_asymmetric_cores(self):
        p=json.loads((ROOT/"profile.json").read_text())
        clusters=p["soc"]["cpu_topology"]["clusters"]
        self.assertEqual(sum(c["count"] for c in clusters), 8)
        self.assertEqual([c["count"] for c in clusters], [4, 4])
        affinity=[mpidr for cluster in clusters for mpidr in cluster["cpu_mpidr_affinity"]]
        self.assertEqual(len(affinity), 8)
        self.assertEqual(len(set(affinity)), 8)
        self.assertTrue(all(value.startswith("0x") for value in affinity))

    def test_no_debian_guest_boot_is_part_of_this_variant(self):
        wf=(ROOT.parents[1]/".github/workflows/snapdragon660-variant.yml").read_text()
        self.assertNotIn("Debian", wf)
        self.assertIn("Snapdragon 660 Variant", wf)
    def test_platform_description_generation(self):
        p=subprocess.run([sys.executable, str(ROOT/"tools/build_platform_description.py")], capture_output=True, text=True)
        self.assertEqual(p.returncode, 0, p.stderr)
        d=json.loads(p.stdout)
        self.assertEqual(d["qemu_machine_type"], "sdm660-lavender")
        self.assertEqual(d["device_tree_scaffold_status"], "native-machine-prototype-unvalidated")
    def test_dts_does_not_claim_verified_silicon_map(self):
        dts=(ROOT/"device-tree/sdm660-lavender.dts").read_text()
        self.assertIn("does not invent Qualcomm MMIO addresses", dts)
        self.assertIn('compatible = "xiaomi,lavender-sdm660", "qcom,sdm660";', dts)

if __name__=="__main__": unittest.main()
