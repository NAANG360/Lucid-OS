#!/usr/bin/env bash
set -euo pipefail
: "${ANDROID_HOME:?ANDROID_HOME must be set}"
TOOLS="$ANDROID_HOME/build-tools/35.0.0"
PLATFORM="$ANDROID_HOME/platforms/android-35/android.jar"
OUT_ROOT="${1:?output directory required}"
mkdir -p "$OUT_ROOT"

ASSET_STAGE="$OUT_ROOT/assets"
mkdir -p "$ASSET_STAGE"
cp -a "$GITHUB_WORKSPACE/theme/assets/." "$ASSET_STAGE/"
if [[ -d "$GITHUB_WORKSPACE/theme/assets-encoded" ]]; then
  for encoded in "$GITHUB_WORKSPACE"/theme/assets-encoded/*.b64; do
    [[ -f "$encoded" ]] || continue
    base64 --decode "$encoded" > "$ASSET_STAGE/$(basename "${encoded%.b64}")"
  done
fi

# Refuse to package a wallpaper picker with blank preview cards. Every entry
# declared by the picker must have a real asset in the staged bundle.
for asset in \
  lucid-wallpaper-dusk.jpg wallpaper-bubbles.webp wallpaper-purple-crescent.webp \
  wallpaper-lucid-crescent.webp lucid-wallpaper-alpine.jpg wallpaper-brushwork.webp; do
  if [[ ! -s "$ASSET_STAGE/$asset" ]]; then
    echo "::error::Missing wallpaper asset: theme/assets/$asset (or its .b64 source under theme/assets-encoded)"
    exit 1
  fi
done

KEYSTORE="$OUT_ROOT/lucid-build.keystore"
if [[ -n "${LUCID_KEYSTORE_BASE64:-}" ]]; then
  printf '%s' "$LUCID_KEYSTORE_BASE64" | base64 --decode > "$KEYSTORE"
  : "${LUCID_KEYSTORE_PASSWORD:?LUCID_KEYSTORE_PASSWORD is required with LUCID_KEYSTORE_BASE64}"
  : "${LUCID_KEY_ALIAS:?LUCID_KEY_ALIAS is required with LUCID_KEYSTORE_BASE64}"
  : "${LUCID_KEY_PASSWORD:?LUCID_KEY_PASSWORD is required with LUCID_KEYSTORE_BASE64}"
  STORE_PASS="$LUCID_KEYSTORE_PASSWORD"; KEY_PASS="$LUCID_KEY_PASSWORD"; KEY_ALIAS="$LUCID_KEY_ALIAS"
  echo "Using configured stable Lucid application signing identity."
else
  keytool -genkeypair -noprompt -keystore "$KEYSTORE" -storepass lucidbuild -keypass lucidbuild \
    -alias lucidbuild -dname "CN=Lucid OS Development,O=Lucid,C=IN" -keyalg RSA -keysize 2048 -validity 10000 >/dev/null 2>&1
  STORE_PASS="lucidbuild"; KEY_PASS="lucidbuild"; KEY_ALIAS="lucidbuild"
  echo "::warning::Ephemeral development key in use. This is not the ROM platform certificate and will not preserve APK update signatures across builds."
fi
printf 'STORE_PASS=%q\nKEY_PASS=%q\nKEY_ALIAS=%q\n' "$STORE_PASS" "$KEY_PASS" "$KEY_ALIAS" > "$OUT_ROOT/signing.env"

build_app() {
  local name="$1" rel="$2" manifest="$3" assets_dir="${4:-}"
  local app="$GITHUB_WORKSPACE/$rel" out="$OUT_ROOT/$name"
  test -f "$app/$manifest"
  mkdir -p "$out/classes" "$out/dex"
  mapfile -t java_files < <(find "$app/src" -type f -name '*.java' -print | sort)
  java_files+=("$GITHUB_WORKSPACE/theme/motion/src/com/lucid/motion/LucidMotion.java")
  test "${#java_files[@]}" -gt 0
  javac -source 8 -target 8 -classpath "$PLATFORM" -d "$out/classes" "${java_files[@]}"
  mapfile -t class_files < <(find "$out/classes" -type f -name '*.class' -print | sort)
  test "${#class_files[@]}" -gt 0
  "$TOOLS/d8" --lib "$PLATFORM" --output "$out/dex" "${class_files[@]}"
  local args=(package -f -M "$app/$manifest" -I "$PLATFORM" -F "$out/$name-unsigned.apk")
  if [[ -d "$app/res" ]]; then args+=(-S "$app/res"); fi
  if [[ -n "$assets_dir" ]]; then
    if [[ -d "$assets_dir" ]]; then args+=(-A "$assets_dir")
    else echo "::error::Asset directory not found: $assets_dir"; exit 1; fi
  elif [[ -d "$app/assets" ]]; then args+=(-A "$app/assets"); fi
  "$TOOLS/aapt" "${args[@]}"
  cp "$out/dex/classes.dex" "$out/classes.dex"
  (cd "$out" && zip -q "$out/$name-unsigned.apk" classes.dex)
  "$TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass "pass:$STORE_PASS" \
    --key-pass "pass:$KEY_PASS" --ks-key-alias "$KEY_ALIAS" --out "$out/$name.apk" "$out/$name-unsigned.apk"
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
build_app Lucid-Wallpapers theme/wallpaper-app AndroidManifest.xml "$ASSET_STAGE"
build_app Lucid-Desktop theme/lucid-desktop AndroidManifest.xml
build_app Lucid-About theme/about-app AndroidManifest.xml "$ASSET_STAGE"

printf '%s\n' \
  "Lucid-Control-Centre.apk" "Lucid-Settings.apk" "Lucid-Launcher.apk" \
  "Lucid-Updater.apk" "Lucid-Wallpapers.apk" "Lucid-Desktop.apk" "Lucid-About.apk" \
  > "$OUT_ROOT/app-inventory.txt"
