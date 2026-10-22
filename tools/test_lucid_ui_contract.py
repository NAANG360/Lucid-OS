#!/usr/bin/env python3
"""Static contract checks for the Lucid UI APK sources; no Android SDK required."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"
errors = []

def check(condition, message):
    if not condition:
        errors.append(message)

def manifest(path, package):
    p = ROOT / path
    try:
        root = ET.parse(p).getroot()
    except Exception as exc:
        errors.append(f"{path}: invalid XML: {exc}")
        return None
    check(root.attrib.get("package") == package, f"{path}: wrong package id")
    return root

cc_path = "theme/control-center-app/AndroidManifest.xml"
cc = manifest(cc_path, "com.lucid.os.controlcenter")
if cc is not None:
    permissions = {n.attrib.get(ANDROID + "name") for n in cc.findall("uses-permission")}
    check("android.permission.CAMERA" in permissions, "Control Center must declare CAMERA for torch permission flow")
    check("android.permission.WRITE_SETTINGS" in permissions, "Control Center must declare WRITE_SETTINGS for brightness access")
    activity = cc.find("application/activity")
    check(activity is not None and activity.attrib.get(ANDROID + "name") == ".MainActivity", "Control Center launcher activity missing")
    service = cc.find("application/service")
    check(service is not None and service.attrib.get(ANDROID + "name") == ".ControlCenterTileService", "Quick Settings tile service missing")
    if service is not None:
        check(service.attrib.get(ANDROID + "permission") == "android.permission.BIND_QUICK_SETTINGS_TILE", "QS tile service must be protected by BIND_QUICK_SETTINGS_TILE")
        actions = {n.attrib.get(ANDROID + "name") for n in service.findall("intent-filter/action")}
        check("android.service.quicksettings.action.QS_TILE" in actions, "QS_TILE action missing")
    check((ROOT / "theme/control-center-app/res/drawable/ic_lucid.xml").is_file(), "Control Center launcher icon missing")
    check((ROOT / "theme/control-center-app/res/drawable/ic_lucid_tile.xml").is_file(), "Quick Settings tile icon missing")

settings_path = "theme/settings-app/AndroidManifest.xml"
settings = manifest(settings_path, "com.lucid.os.settings")
if settings is not None:
    activity = settings.find("application/activity")
    check(activity is not None and activity.attrib.get(ANDROID + "name") == ".MainActivity", "Lucid Settings launcher activity missing")
    check((ROOT / "theme/settings-app/res/drawable/ic_lucid.xml").is_file(), "Settings launcher icon missing")

cc_java = (ROOT / "theme/control-center-app/src/com/lucid/os/controlcenter/MainActivity.java").read_text()
for token in ("Settings.System.SCREEN_BRIGHTNESS", "audio.setStreamVolume", "camera.setTorchMode", "Settings.ACTION_DISPLAY_SETTINGS", "Settings.ACTION_BLUETOOTH_SETTINGS"):
    check(token in cc_java, f"Control Center missing expected real control/intent: {token}")
tile_java = (ROOT / "theme/control-center-app/src/com/lucid/os/controlcenter/ControlCenterTileService.java").read_text()
check("startActivityAndCollapse" in tile_java, "QS tile must open the Control Center")
settings_java = (ROOT / "theme/settings-app/src/com/lucid/os/settings/MainActivity.java").read_text()
for token in ("Settings.ACTION_DISPLAY_SETTINGS", "Settings.ACTION_SOUND_SETTINGS", "com.lucid.os.wallpapers", "com.lucid.os.controlcenter"):
    check(token in settings_java, f"Lucid Settings missing expected destination: {token}")

boundary_doc = (ROOT / "theme/systemui/INTEGRATION_PLAN.md").read_text()
check("does not replace the stock shade" in boundary_doc, "Document the boundary between app-level Control Center and SystemUI shade")

if errors:
    print("Lucid UI source contract: FAIL")
    for error in errors:
        print(" -", error)
    sys.exit(1)
print("Lucid UI source contract: PASS")
