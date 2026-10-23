#!/usr/bin/env bash
# Build the LucidOS first-boot onboarding APK with Android SDK build-tools.
# Usage: ANDROID_HOME=/path/to/sdk tools/build_lucid_setup.sh [output.apk]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-}"
if [[ -z "$SDK" || ! -d "$SDK" ]]; then
  echo "Set ANDROID_HOME to an installed Android SDK." >&2
  exit 2
fi

TOOLS="$SDK/build-tools/35.0.0"
ANDROID_JAR="$SDK/platforms/android-35/android.jar"
OUT="$ROOT/out/LucidSetup.apk"
if [[ "$#" -ge 1 ]]; then OUT="$1"; fi
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

for tool in aapt2 aapt apksigner zipalign d8; do
  [[ -x "$TOOLS/$tool" ]] || { echo "Missing Android build tool: $TOOLS/$tool" >&2; exit 2; }
done
[[ -f "$ANDROID_JAR" ]] || { echo "Missing Android platform: $ANDROID_JAR" >&2; exit 2; }
command -v javac >/dev/null
command -v keytool >/dev/null
command -v zip >/dev/null

mkdir -p "$TMP/classes" "$TMP/dex" "$TMP/assets" "$(dirname "$OUT")"
cp "$ROOT/theme/assets/lucid-wallpaper-dusk.jpg" "$TMP/assets/"
cp "$ROOT/theme/assets/lucid-wallpaper-alpine.jpg" "$TMP/assets/"

"$TOOLS/aapt2" compile --dir "$ROOT/setup-app/res" -o "$TMP/resources.zip"
"$TOOLS/aapt2" link --manifest "$ROOT/setup-app/AndroidManifest.xml" -I "$ANDROID_JAR" -A "$TMP/assets" -o "$TMP/LucidSetup-unsigned.apk" "$TMP/resources.zip"

mapfile -t JAVA_SOURCES < <(find "$ROOT/setup-app/src" -name '*.java' -print | sort)
if [[ -z "${JAVA_SOURCES[*]:-}" ]]; then echo "No setup-app Java sources found." >&2; exit 2; fi
javac --release 8 -classpath "$ANDROID_JAR" -d "$TMP/classes" "${JAVA_SOURCES[@]}"
mapfile -t CLASS_FILES < <(find "$TMP/classes" -name '*.class' -print | sort)
"$TOOLS/d8" --lib "$ANDROID_JAR" --output "$TMP/dex" "${CLASS_FILES[@]}"
(cd "$TMP/dex" && zip -q "$TMP/LucidSetup-unsigned.apk" classes.dex)

keytool -genkeypair -noprompt -keystore "$TMP/setup.jks" -storepass 'lucid-setup-build' -keypass 'lucid-setup-build' -alias lucid-setup -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=LucidOS Setup Development'
"$TOOLS/zipalign" -f 4 "$TMP/LucidSetup-unsigned.apk" "$TMP/LucidSetup-aligned.apk"
"$TOOLS/apksigner" sign --ks "$TMP/setup.jks" --ks-key-alias lucid-setup --ks-pass pass:lucid-setup-build --key-pass pass:lucid-setup-build --out "$OUT" "$TMP/LucidSetup-aligned.apk"
"$TOOLS/apksigner" verify --verbose "$OUT"
"$TOOLS/aapt" dump badging "$OUT" | tee "$TMP/badging.txt"
grep -q "package: name='com.cosmos.setup'" "$TMP/badging.txt"
grep -q 'android.intent.category.SETUP_WIZARD' "$ROOT/setup-app/AndroidManifest.xml"
grep -q 'android.permission.WRITE_SECURE_SETTINGS' "$ROOT/setup-app/AndroidManifest.xml"
grep -q '<privapp-permissions package="com.cosmos.setup">' "$ROOT/system-tree/system/etc/permissions/lucid-privapp-permissions.xml"
grep -q 'lucid_setup_complete' "$ROOT/system-tree/system/bin/lucid_firstboot.sh"
grep -q 'lucid_setup_complete' "$ROOT/setup-app/src/com/cosmos/setup/MainActivity.java"
sha256sum "$OUT"
echo "Built and verified $OUT"
echo "NOTE: development signature only; follow ROM signing policy and verify privileged permission grant before integration."
