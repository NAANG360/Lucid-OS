#!/usr/bin/env bash
set -uo pipefail
mkdir -p "$RUNNER_TEMP/runtime-logs/screenshots"
adb wait-for-device
adb shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'
adb shell settings put global window_animation_scale 0 || true
adb shell settings put global transition_animation_scale 0 || true
adb shell settings put global animator_duration_scale 0 || true
adb shell input keyevent 82 || true

fail=0
test_app() {
  local label="$1" package="$2" apk="$3"
  local logfile="$RUNNER_TEMP/runtime-logs/$label.log"
  echo "===== $label ($package) ====="
  if ! adb install -r "$apk" > "$RUNNER_TEMP/runtime-logs/$label-install.txt" 2>&1; then
    cat "$RUNNER_TEMP/runtime-logs/$label-install.txt"
    echo "INSTALL_FAILED $label" | tee "$logfile"
    fail=1
    return
  fi
  adb logcat -c
  local launch
  launch="$(adb shell monkey -p "$package" -c android.intent.category.LAUNCHER 1 2>&1)"
  printf '%s\n' "$launch" | tee "$logfile"
  sleep 15
  adb logcat -d -v threadtime > "$RUNNER_TEMP/runtime-logs/$label-logcat.txt"
  if printf '%s' "$launch" | grep -Eqi 'No activities found|monkey aborted|Error:'; then
    echo "LAUNCH_FAILED $label" | tee -a "$logfile"
    fail=1
  fi
  if grep -E -B 5 -A 12 "FATAL EXCEPTION|Fatal signal|Process: $package|am_crash.*$package" "$RUNNER_TEMP/runtime-logs/$label-logcat.txt" > "$RUNNER_TEMP/runtime-logs/$label-crash.txt"; then
    echo "CRASH_DETECTED $label" | tee -a "$logfile"
    cat "$RUNNER_TEMP/runtime-logs/$label-crash.txt" | tee -a "$logfile"
    fail=1
  else
    echo "No fatal crash signature detected for $label after 15 seconds." | tee -a "$logfile"
  fi
  adb exec-out screencap -p > "$RUNNER_TEMP/runtime-logs/screenshots/$label.png" || true
  adb shell dumpsys activity activities > "$RUNNER_TEMP/runtime-logs/$label-activity.txt" || true
  adb shell am force-stop "$package" || true
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
grep -H -E 'No fatal crash|CRASH_DETECTED|LAUNCH_FAILED|INSTALL_FAILED' "$RUNNER_TEMP/runtime-logs"/*.log || true
# Verify that all seven screenshot files exist and are non-empty.
for app in control-centre settings launcher updater wallpapers desktop dynamic-island; do
  test -s "$RUNNER_TEMP/runtime-logs/screenshots/$app.png" || { echo "SCREENSHOT_MISSING $app"; fail=1; }
done
exit "$fail"
