#!/usr/bin/env bash
set -euo pipefail
OUT="${1:-out/LucidAbout.apk}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:?ANDROID_HOME is required}"
BT="$SDK/build-tools/35.0.0"
WORK="$ROOT/out/about-build"
rm -rf "$WORK"
mkdir -p "$WORK/res/drawable" "$WORK/classes" "$(dirname "$OUT")"
cp "$ROOT/theme/assets/lucid-wallpaper-dusk.jpg" "$WORK/res/drawable/lucid_wallpaper.jpg"
"$BT/aapt2" compile --dir "$WORK/res" -o "$WORK/resources.zip"
"$BT/aapt2" link -o "$WORK/unsigned.apk" -I "$SDK/platforms/android-35/android.jar" --manifest "$ROOT/theme/about-app/AndroidManifest.xml" --java "$WORK/generated" "$WORK/resources.zip"
find "$ROOT/theme/about-app/src" "$WORK/generated" -name '*.java' -print0 | xargs -0 javac -source 8 -target 8 -classpath "$SDK/platforms/android-35/android.jar" -d "$WORK/classes"
"$BT/d8" --min-api 29 --lib "$SDK/platforms/android-35/android.jar" --output "$WORK/dex" "$WORK/classes"/*.class
mkdir -p "$WORK/apk"
cp "$WORK/unsigned.apk" "$WORK/apk/base.apk"
(cd "$WORK/apk" && "$SDK/platform-tools/../build-tools/35.0.0/zipalign" -f 4 base.apk aligned.apk)
KEY="$WORK/about.jks"
keytool -genkeypair -noprompt -keystore "$KEY" -storepass 'lucid-about-build' -keypass 'lucid-about-build' -alias lucid-about -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=LucidOS About Development'
"$BT/apksigner" sign --ks "$KEY" --ks-key-alias lucid-about --ks-pass pass:lucid-about-build --key-pass pass:lucid-about-build --out "$OUT" "$WORK/apk/aligned.apk"
"$BT/apksigner" verify --verbose "$OUT"
