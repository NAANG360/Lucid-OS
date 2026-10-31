#!/usr/bin/env bash
set -euo pipefail
mkdir -p report/emulator/screenshots report/emulator/ui
adb wait-for-device
for attempt in $(seq 1 30); do
  if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then break; fi
  sleep 2
done
test "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1"
adb shell settings put system font_scale 1.0
adb shell wm size 1080x1920
adb shell wm density 420
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

capture_app() {
  local name="$1" component="$2" pkg="$3" apk="out/lucid-apps/$1.apk"
  test -s "$apk"
  adb install -r "$apk"
  adb shell am force-stop "$pkg" || true
  adb shell am start -W -n "$component" | tee "report/emulator/ui/$name-launch.txt"
  sleep 5
  adb shell dumpsys activity activities > "report/emulator/ui/$name-activities.txt"
  if ! grep -q "$pkg" "report/emulator/ui/$name-activities.txt"; then
    echo "Activity did not remain foreground: $name ($pkg)" >&2
    adb logcat -d -t 3000 > "report/emulator/ui/$name-logcat.txt"
    return 1
  fi
  adb shell uiautomator dump "/sdcard/$name.xml" >/dev/null
  adb pull "/sdcard/$name.xml" "report/emulator/ui/$name.xml"
  adb shell screencap -p "/sdcard/$name.png"
  adb pull "/sdcard/$name.png" "report/emulator/screenshots/$name.png"
  adb shell input keyevent 4 || true
  sleep 1
}

capture_app Lucid-Launcher com.lucid.os.launcher/.MainActivity com.lucid.os.launcher
capture_app Lucid-Control-Centre com.lucid.os.controlcentre/.MainActivity com.lucid.os.controlcentre
capture_app Lucid-Settings com.lucid.os.settings/.MainActivity com.lucid.os.settings
capture_app Lucid-Updater com.lucid.os.updater/.MainActivity com.lucid.os.updater
capture_app Lucid-Wallpapers com.lucid.os.wallpaper/.MainActivity com.lucid.os.wallpaper
capture_app Lucid-Desktop com.lucid.os.desktop/.MainActivity com.lucid.os.desktop
capture_app Lucid-About com.lucid.os.about/.MainActivity com.lucid.os.about

adb shell input keyevent HOME || true
sleep 2
adb shell screencap -p /sdcard/lucid-emulator-home.png
adb pull /sdcard/lucid-emulator-home.png report/emulator/screenshots/Lucid-Emulator-Home.png
adb logcat -d -t 12000 > report/emulator/logcat.txt
if grep -E 'FATAL EXCEPTION|Process: com\\.lucid\\.os\\.' report/emulator/logcat.txt; then
  echo "One or more Lucid apps crashed during emulator pass." >&2
  exit 1
fi
echo "PASS: rebuilt seven APKs installed and all seven activities launched; screenshots and UI hierarchy captured." | tee report/emulator/result.txt
