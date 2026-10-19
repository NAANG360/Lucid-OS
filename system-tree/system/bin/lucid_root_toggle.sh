#!/system/bin/sh
# Lucid's su gate lives on the read-only system partition in the current image.
# Change its executable mode only when the filesystem can be remounted safely;
# never claim success if Android's verified/dynamic mount policy refuses it.

TAG="LucidRootGate"
SU_PATH="/system/xbin/su"
STATUS="$(getprop persist.lucid.root)"

log -t "$TAG" "Requested root gate state: $STATUS"

if [ ! -e "$SU_PATH" ]; then
    log -t "$TAG" "Root binary missing at $SU_PATH; cannot change root availability."
    exit 1
fi

if ! mount -o remount,rw /system; then
    log -t "$TAG" "Could not remount /system read-write; root gate unchanged."
    exit 1
fi

if [ "$STATUS" = "1" ]; then
    # Android su binaries conventionally need setuid/setgid; 0755 does not
    # elevate a caller and was a functional bug in the previous implementation.
    chown 0:2000 "$SU_PATH" && chmod 06755 "$SU_PATH"
    RESULT=$?
else
    chmod 0000 "$SU_PATH"
    RESULT=$?
fi

# Always attempt to restore the normal read-only mount, including on failure.
if ! mount -o remount,ro /system; then
    log -t "$TAG" "WARNING: failed to restore /system read-only after root-gate change."
    RESULT=1
fi

if [ "$RESULT" -ne 0 ]; then
    log -t "$TAG" "Failed to change su permissions; requested state was not applied."
    exit 1
fi

MODE="$(stat -c '%a' "$SU_PATH" 2>/dev/null)"
if [ "$STATUS" = "1" ] && [ "$MODE" != "6755" ]; then
    log -t "$TAG" "Expected su mode 6755 after enabling root, got '$MODE'."
    exit 1
fi
if [ "$STATUS" != "1" ] && [ "$MODE" != "0" ]; then
    log -t "$TAG" "Expected su mode 0 after disabling root, got '$MODE'."
    exit 1
fi
log -t "$TAG" "Root gate updated; su mode=$MODE (requested state=$STATUS)."
exit 0
