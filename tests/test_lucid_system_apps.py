from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
class LucidSystemAppsTests(unittest.TestCase):
 def test_custom_wallpaper_and_system_entry(self):
  s=(ROOT/"theme/wallpaper-app/src/com/lucid/os/wallpapers/MainActivity.java").read_text()
  m=(ROOT/"theme/wallpaper-app/AndroidManifest.xml").read_text()
  self.assertIn("Intent.ACTION_OPEN_DOCUMENT",s); self.assertIn('i.setType("image/*")',s)
  self.assertIn("WallpaperManager.FLAG_SYSTEM",s); self.assertIn("android.intent.action.SET_WALLPAPER",m)
  self.assertIn("LucidPalette.apply(this,seed)",s)
 def test_figma_art_assets_are_used(self):
  s=(ROOT/"theme/wallpaper-app/src/com/lucid/os/wallpapers/MainActivity.java").read_text()
  for name in ("lucid-wallpaper-dusk.jpg","lucid-wallpaper-alpine.jpg"):
   self.assertIn(name,s); self.assertTrue((ROOT/"theme/assets"/name).is_file())
  self.assertNotRegex(s,r"https?://")
 def test_about_uses_real_build_values_and_copy(self):
  s=(ROOT/"theme/about-app/src/com/lucid/os/about/MainActivity.java").read_text()
  for v in ("Build.MODEL","Build.DEVICE","Build.VERSION.RELEASE","Build.VERSION.SDK_INT","Build.FINGERPRINT","Build.VERSION.SECURITY_PATCH"): self.assertIn(v,s)
  self.assertIn("ClipboardManager",s)
 def test_root_script_does_not_fake_suid_root(self):
  s=(ROOT/"system-tree/system/bin/lucid_root_toggle.sh").read_text()
  self.assertIn("sys.lucid.root_available",s); self.assertIn("magisk",s)
  self.assertNotIn("chmod 06755",s); self.assertNotIn("mount -o remount,rw /system",s)
  self.assertNotIn("on property:persist.lucid.root=*", (ROOT/"system-tree/system/etc/init/init.lucid.rc").read_text())
 def test_theme_pack_has_both_apps_and_main_branch_guard(self):
  s=(ROOT/".github/workflows/lucid-theme-pack.yml").read_text()
  self.assertIn("LucidWallpapers.apk",s); self.assertIn("LucidAbout.apk",s)
  self.assertIn("workflow_run.head_branch == 'main'",s)
if __name__=="__main__": unittest.main()