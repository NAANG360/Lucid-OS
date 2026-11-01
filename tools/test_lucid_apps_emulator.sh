#!/usr/bin/env bash
set -euo pipefail
mkdir -p report/emulator/screenshots report/emulator/ui
adb wait-for-device
for attempt in $(seq 1 60); do
  [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]] && break
  sleep 2
done
test "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1"
adb shell settings put system font_scale 1.0
adb shell wm size 1080x1920
adb shell wm density 420
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell cmd package set-home-activity com.lucid.os.launcher/.MainActivity || true

capture_app() {
  local name="$1" component="$2" pkg="$3" apk="out/lucid-apps/$1.apk"
  test -s "$apk"
  adb install -r "$apk"
  adb shell am force-stop "$pkg" || true
  adb logcat -c
  adb shell am start -W -n "$component" > "report/emulator/ui/$name-launch.txt" 2>&1 || {
    cat "report/emulator/ui/$name-launch.txt"
    return 1
  }
  sleep 4
  adb shell dumpsys activity activities > "report/emulator/ui/$name-activities.txt"
  if ! grep -q "$pkg" "report/emulator/ui/$name-activities.txt"; then
    adb logcat -d -t 5000 > "report/emulator/ui/$name-logcat.txt"
    echo "Activity did not remain foreground: $name ($pkg)" >&2
    return 1
  fi
  adb shell screencap -p "/data/local/tmp/$name.png"
  adb pull "/data/local/tmp/$name.png" "report/emulator/screenshots/$name.png" >/dev/null
  adb logcat -d -t 8000 > "report/emulator/ui/$name-logcat.txt"
  if grep -Eiq 'ANR in |Input dispatching timed out|Process system isn.t responding|Application Not Responding|FATAL EXCEPTION' "report/emulator/ui/$name-logcat.txt"; then
    echo "ANR or crash detected while testing $name" >&2
    return 1
  fi
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
adb shell screencap -p /data/local/tmp/lucid-emulator-home.png
adb pull /data/local/tmp/lucid-emulator-home.png report/emulator/screenshots/Lucid-Emulator-Home.png >/dev/null
adb logcat -d -t 12000 > report/emulator/logcat.txt

python3 - <<'PY'
from PIL import Image, ImageStat
from pathlib import Path
for path in sorted(Path("report/emulator/screenshots").glob("*.png")):
    image = Image.open(path).convert("RGB")
    stat = ImageStat.Stat(image)
    mean = sum(stat.mean) / 3
    extrema = image.getextrema()
    bright = sum(1 for pixel in image.resize((135, 240)).getdata()
                 if max(pixel) > 65)
    if mean < 3.0 or bright < 80:
        raise SystemExit(f"FAIL: {path.name} appears blank/black (mean={mean:.2f}, visible_samples={bright})")
    print(f"SCREEN OK: {path.name} mean={mean:.1f} range={extrema}")
PY
echo "PASS: seven APKs installed; activities foregrounded; no ANR/crash signatures; screenshots contain visible UI." | tee report/emulator/result.txt
