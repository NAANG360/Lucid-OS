#!/usr/bin/env bash
set -euo pipefail
: "${ANDROID_HOME:?ANDROID_HOME must be set}"
TOOLS="$ANDROID_HOME/build-tools/35.0.0"
PLATFORM="$ANDROID_HOME/platforms/android-35/android.jar"
OUT_ROOT="${1:?output directory required}"
mkdir -p "$OUT_ROOT"
KEYSTORE="$OUT_ROOT/lucid-build.keystore"
keytool -genkeypair -noprompt -keystore "$KEYSTORE" -storepass lucidbuild -keypass lucidbuild -alias lucidbuild -dname "CN=Lucid OS CI,O=Lucid,C=IN" -keyalg RSA -keysize 2048 -validity 10000 >/dev/null 2>&1
build_app() {
  local name="$1" rel="$2" manifest="$3"
  local app="$GITHUB_WORKSPACE/$rel" out="$OUT_ROOT/$name"
  mkdir -p "$out/classes" "$out/dex"
  mapfile -t java_files < <(find "$app/src" -type f -name '*.java' -print | sort)
  test "${#java_files[@]}" -gt 0
  javac -source 8 -target 8 -classpath "$PLATFORM" -d "$out/classes" "${java_files[@]}"
  mapfile -t class_files < <(find "$out/classes" -type f -name '*.class' -print | sort)
  test "${#class_files[@]}" -gt 0
  "$TOOLS/d8" --lib "$PLATFORM" --output "$out/dex" "${class_files[@]}"
  local args=(package -f -M "$app/$manifest" -I "$PLATFORM" -F "$out/$name-unsigned.apk")
  if [[ -d "$app/res" ]]; then args+=(-S "$app/res"); fi
  if [[ -d "$app/assets" ]]; then args+=(-A "$app/assets"); fi
  "$TOOLS/aapt" "${args[@]}"
  cp "$out/dex/classes.dex" "$out/classes.dex"
  (cd "$out" && zip -q "$out/$name-unsigned.apk" classes.dex)
  "$TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass pass:lucidbuild --key-pass pass:lucidbuild --out "$out/$name.apk" "$out/$name-unsigned.apk"
  "$TOOLS/apksigner" verify --verbose "$out/$name.apk"
  "$TOOLS/aapt" dump badging "$out/$name.apk" > "$out/badging.txt"
  test -s "$out/$name.apk"
  cp "$out/$name.apk" "$OUT_ROOT/$name.apk"
}
build_app Lucid-Control-Centre theme/lucid-control-centre AndroidManifest.xml
build_app Lucid-Settings theme/lucid-settings AndroidManifest.xml
build_app Lucid-Launcher theme/lucid-launcher AndroidManifest.xml
build_app Lucid-Updater theme/lucid-updater AndroidManifest.xml
build_app Lucid-Wallpapers theme/lucid-wallpaper AndroidManifest.xml
build_app Lucid-Desktop theme/lucid-desktop AndroidManifest.xml
