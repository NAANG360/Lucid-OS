#!/usr/bin/env bash
set -uo pipefail
LOG_DIR="$RUNNER_TEMP/runtime-logs"
SCREEN_DIR="$LOG_DIR/screenshots"
mkdir -p "$SCREEN_DIR"
fail=0

capture_screen() {
  local label="$1"
  local remote="/sdcard/lucid-$label.png"
  local target="$SCREEN_DIR/$label.png"
  rm -f "$target"
  if adb shell screencap -p "$remote" >/dev/null 2>&1 && adb pull "$remote" "$target" >/dev/null 2>&1; then
    if python3 - "$target" <<'PY'
import struct, sys
p=sys.argv[1]
with open(p,'rb') as f:
    h=f.read(24)
if len(h) < 24 or h[:8] != b'\x89PNG\r\n\x1a\n':
    raise SystemExit(1)
w,hgt=struct.unpack(">II",h[16:24])
if w < 200 or hgt < 200:
    raise SystemExit(1)
PY
    then
      echo "SCREENSHOT_OK $label $(stat -c%s "$target") bytes"
    else
      echo "SCREENSHOT_INVALID $label"
      rm -f "$target"
      fail=1
    fi
  else
    echo "SCREENSHOT_CAPTURE_FAILED $label"
    fail=1
  fi
  adb shell rm -f "$remote" >/dev/null 2>&1 || true
}

test_app() {
  local label="$1" package="$2" apk="$3"
  local logfile="$LOG_DIR/$label.log"
  echo "===== $label ($package) ====="
  if ! adb install -r "$apk" > "$LOG_DIR/$label-install.txt" 2>&1; then
    cat "$LOG_DIR/$label-install.txt"
    echo "INSTALL_FAILED $label" | tee "$logfile"
    fail=1
    return
  fi
  adb logcat -c || true
  local launch
  launch="$(adb shell monkey -p "$package" -c android.intent.category.LAUNCHER 1 2>&1)"
  printf '%s\n' "$launch" | tee "$logfile"
  if printf '%s' "$launch" | grep -Eqi 'No activities found|monkey aborted|Error:'; then
    echo "LAUNCH_FAILED $label" | tee -a "$logfile"
    fail=1
  fi
  sleep 15
  adb logcat -d -v threadtime > "$LOG_DIR/$label-logcat.txt" 2>&1 || true
  if grep -E -B 5 -A 12 "FATAL EXCEPTION|Fatal signal|Process: $package|am_crash.*$package" "$LOG_DIR/$label-logcat.txt" > "$LOG_DIR/$label-crash.txt"; then
    echo "CRASH_DETECTED $label" | tee -a "$logfile"
    cat "$LOG_DIR/$label-crash.txt" | tee -a "$logfile"
    fail=1
  else
    echo "No fatal crash signature detected for $label after 15 seconds." | tee -a "$logfile"
  fi
  capture_screen "$label"
  adb shell dumpsys activity activities > "$LOG_DIR/$label-activity.txt" 2>&1 || true
  adb shell am force-stop "$package" >/dev/null 2>&1 || true
  sleep 2
}

test_app control-centre com.lucid.os.controlcentre "$RUNNER_TEMP/lucid-apps/Lucid-Control-Centre.apk"
test_app settings com.lucid.os.settings "$RUNNER_TEMP/lucid-apps/Lucid-Settings.apk"
test_app launcher com.lucid.os.launcher "$RUNNER_TEMP/lucid-apps/Lucid-Launcher.apk"
test_app updater com.lucid.os.updater "$RUNNER_TEMP/lucid-apps/Lucid-Updater.apk"
test_app wallpapers com.lucid.os.wallpaper "$RUNNER_TEMP/lucid-apps/Lucid-Wallpapers.apk"
test_app desktop com.lucid.os.desktop "$RUNNER_TEMP/lucid-apps/Lucid-Desktop.apk"
test_app dynamic-island com.lucid.os.island "$RUNNER_TEMP/lucid-apps/Lucid-Dynamic-Island.apk"

echo "===== Runtime smoke summary ====="
grep -H -E 'No fatal crash|CRASH_DETECTED|LAUNCH_FAILED|INSTALL_FAILED' "$LOG_DIR"/*.log || true
count=0
for app in control-centre settings launcher updater wallpapers desktop dynamic-island; do
  if [ -s "$SCREEN_DIR/$app.png" ]; then count=$((count+1)); else echo "SCREENSHOT_MISSING $app"; fi
done
echo "SCREENSHOTS_CAPTURED $count/7"
[ "$count" -eq 7 ] || fail=1
exit "$fail"
