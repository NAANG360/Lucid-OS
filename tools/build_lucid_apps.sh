#!/usr/bin/env bash
set -euo pipefail

: "${ANDROID_HOME:?ANDROID_HOME must be set}"
TOOLS="$ANDROID_HOME/build-tools/35.0.0"
PLATFORM="$ANDROID_HOME/platforms/android-35/android.jar"
OUT_ROOT="${1:?output directory required}"
mkdir -p "$OUT_ROOT"

# CI signing is deliberately explicit. A stable keystore can be supplied by
# repository secrets; otherwise the generated key is development-only and
# must never be treated as the ROM platform certificate.
KEYSTORE="$OUT_ROOT/lucid-build.keystore"
if [[ -n "${LUCID_KEYSTORE_BASE64:-}" ]]; then
  printf '%s' "$LUCID_KEYSTORE_BASE64" | base64 --decode > "$KEYSTORE"
  : "${LUCID_KEYSTORE_PASSWORD:?LUCID_KEYSTORE_PASSWORD is required with LUCID_KEYSTORE_BASE64}"
  : "${LUCID_KEY_ALIAS:?LUCID_KEY_ALIAS is required with LUCID_KEYSTORE_BASE64}"
  : "${LUCID_KEY_PASSWORD:?LUCID_KEY_PASSWORD is required with LUCID_KEYSTORE_BASE64}"
  STORE_PASS="$LUCID_KEYSTORE_PASSWORD"
  KEY_PASS="$LUCID_KEY_PASSWORD"
  KEY_ALIAS="$LUCID_KEY_ALIAS"
  echo "Using configured stable Lucid application signing identity."
else
  keytool -genkeypair -noprompt -keystore "$KEYSTORE" -storepass lucidbuild -keypass lucidbuild \
    -alias lucidbuild -dname "CN=Lucid OS Development,O=Lucid,C=IN" \
    -keyalg RSA -keysize 2048 -validity 10000 >/dev/null 2>&1
  STORE_PASS="lucidbuild"
  KEY_PASS="lucidbuild"
  KEY_ALIAS="lucidbuild"
  echo "::warning::Using an ephemeral development signing key. APK signatures will change on the next run; this is not a platform signing key."
fi

build_app() {
  local name="$1" rel="$2" manifest="$3" assets_dir="${4:-}"
  local app="$GITHUB_WORKSPACE/$rel" out="$OUT_ROOT/$name"
  test -f "$app/$manifest"
  mkdir -p "$out/classes" "$out/dex"
  mapfile -t java_files < <(find "$app/src" -type f -name '*.java' -print | sort)
  test "${#java_files[@]}" -gt 0
  javac -source 8 -target 8 -classpath "$PLATFORM" -d "$out/classes" "${java_files[@]}"
  mapfile -t class_files < <(find "$out/classes" -type f -name '*.class' -print | sort)
  test "${#class_files[@]}" -gt 0
  "$TOOLS/d8" --lib "$PLATFORM" --output "$out/dex" "${class_files[@]}"

  local args=(package -f -M "$app/$manifest" -I "$PLATFORM" -F "$out/$name-unsigned.apk")
  if [[ -d "$app/res" ]]; then args+=(-S "$app/res"); fi
  if [[ -n "$assets_dir" ]]; then
    test -d "$GITHUB_WORKSPACE/$assets_dir"
    args+=(-A "$GITHUB_WORKSPACE/$assets_dir")
  elif [[ -d "$app/assets" ]]; then
    args+=(-A "$app/assets")
  fi
  "$TOOLS/aapt" "${args[@]}"
  cp "$out/dex/classes.dex" "$out/classes.dex"
  (cd "$out" && zip -q "$out/$name-unsigned.apk" classes.dex)
  "$TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass "pass:$STORE_PASS" \
    --key-pass "pass:$KEY_PASS" --ks-key-alias "$KEY_ALIAS" \
    --out "$out/$name.apk" "$out/$name-unsigned.apk"
  "$TOOLS/apksigner" verify --verbose "$out/$name.apk"
  "$TOOLS/aapt" dump badging "$out/$name.apk" > "$out/badging.txt"
  grep -q "^package: name=" "$out/badging.txt"
  test -s "$out/$name.apk"
  cp "$out/$name.apk" "$OUT_ROOT/$name.apk"
}

build_app Lucid-Control-Centre theme/lucid-control-centre AndroidManifest.xml
build_app Lucid-Settings theme/lucid-settings AndroidManifest.xml
build_app Lucid-Launcher theme/lucid-launcher AndroidManifest.xml
build_app Lucid-Updater theme/lucid-updater AndroidManifest.xml
# Use the full supplied-artwork wallpaper picker, not the four-generated-style
# prototype in theme/lucid-wallpaper.
build_app Lucid-Wallpapers theme/wallpaper-app AndroidManifest.xml theme/assets
build_app Lucid-Desktop theme/lucid-desktop AndroidManifest.xml
# Lucid About is a separately maintained app and is mandatory in every ROM build.
build_app Lucid-About theme/about-app AndroidManifest.xml theme/assets

printf '%s\n' \
  "Lucid-Control-Centre.apk" "Lucid-Settings.apk" "Lucid-Launcher.apk" \
  "Lucid-Updater.apk" "Lucid-Wallpapers.apk" "Lucid-Desktop.apk" \
  "Lucid-About.apk" > "$OUT_ROOT/app-inventory.txt"
