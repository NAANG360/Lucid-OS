#!/usr/bin/env python3
"""Tests for explicit Lucid overlay staging."""
import importlib.util
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).resolve().parents[1] / "tools" / "stage_lucid_overlays.py"
SPEC = importlib.util.spec_from_file_location("stage_lucid_overlays", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(MODULE)


class OverlayInventoryTests(unittest.TestCase):
    def test_inventory_distinguishes_add_replace_and_unchanged(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            src, dst = root / "src", root / "dst"
            src.mkdir()
            dst.mkdir()
            (src / "same").write_text("same")
            (src / "replace").write_text("new")
            (src / "add").write_text("new")
            (dst / "same").write_text("same")
            (dst / "replace").write_text("old")
            rows = MODULE.inventory(src, dst)
            actions = {Path(row["destination"]).name: row["action"] for row in rows}
            self.assertEqual(actions, {"same": "unchanged", "replace": "replace", "add": "add"})
            self.assertTrue(all(len(row["sha256"]) == 64 for row in rows))
            self.assertTrue(all("mode" in row for row in rows))

    def test_allowlist_never_overlays_android_framework_settings_or_systemui(self):
        destinations = [dest for _, dest, _ in MODULE.OVERLAYS]
        self.assertFalse(any("/framework/" in dest for dest in destinations))
        self.assertFalse(any(dest.endswith("/Settings.apk") or dest.endswith("/SystemUI.apk")
                             for dest in destinations))
        self.assertIn("system/etc/init/init.lucid.rc", destinations)
        self.assertIn("system/bin/lucid-linux-app", destinations)

    def test_source_tree_symlink_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            src, outside = root / "src", root / "outside"
            src.mkdir()
            outside.write_text("do not copy")
            (src / "escape").symlink_to(outside)
            with self.assertRaisesRegex(ValueError, "symlink"):
                MODULE.source_files(src)

    def test_destination_symlink_components_are_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            base, outside = root / "base", root / "outside"
            base.mkdir()
            outside.mkdir()
            (base / "system").symlink_to(outside, target_is_directory=True)
            with self.assertRaisesRegex(ValueError, "symlink"):
                MODULE.reject_symlink_components(base / "system" / "etc", base)

    def test_extra_overlay_allows_only_reviewed_destinations(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            artifact = root / "artifact.apk"
            artifact.write_text("apk")
            source, destination = MODULE.parse_extra_overlay(
                f"{artifact}=system/app/LucidWallpapers/LucidWallpapers.apk"
            )
            self.assertEqual(source, artifact)
            self.assertEqual(destination, "system/app/LucidWallpapers/LucidWallpapers.apk")
            directory = root / "artifact-dir"
            directory.mkdir()
            with self.assertRaisesRegex(ValueError, "regular file"):
                MODULE.parse_extra_overlay(f"{directory}=system/media/bootanimation.zip")
            with self.assertRaisesRegex(ValueError, "not approved"):
                MODULE.parse_extra_overlay(f"{artifact}=system/framework/framework.jar")
            with self.assertRaisesRegex(ValueError, "not approved"):
                MODULE.parse_extra_overlay(f"{artifact}=system/priv-app/SystemUI/SystemUI.apk")

    def test_external_source_symlink_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / "target"
            target.write_text("artifact")
            link = root / "artifact-link"
            link.symlink_to(target)
            with self.assertRaisesRegex(ValueError, "symlink"):
                MODULE.safe_source_path(str(link))

    def test_path_outside_root_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            with self.assertRaisesRegex(ValueError, "escapes"):
                MODULE.reject_symlink_components(root.parent / "escape", root)


if __name__ == "__main__":
    unittest.main()
