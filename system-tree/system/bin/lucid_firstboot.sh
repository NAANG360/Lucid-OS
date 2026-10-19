#!/system/bin/sh

TAG="LucidSetup"
PACKAGE="com.cosmos.setup"
DONE="$(settings get secure lucid_setup_complete 2>/dev/null)"
MARKER="/data/system/lucid_setup_complete"

# Keep compatibility with older builds that used the marker file.
if [ "$DONE" = "1" ] || [ -f "$MARKER" ]; then
    log -t "$TAG" "First-boot setup already completed."
    exit 0
fi

# Wait for Package Manager and the privileged setup APK to become available.
ATTEMPT=0
while [ "$ATTEMPT" -lt 30 ]; do
    APK_PATH="$(cmd package path "$PACKAGE" 2>/dev/null | head -n 1)"
    if [ -n "$APK_PATH" ]; then
        break
    fi
    ATTEMPT=$((ATTEMPT + 1))
    sleep 2
done

if [ -z "$APK_PATH" ]; then
    log -t "$TAG" "Setup package $PACKAGE is not installed; leaving setup incomplete for the next boot."
    exit 1
fi

ACTIVITY="$(cmd package resolve-activity --brief --user 0 \
    -a android.intent.action.MAIN \
    -c android.intent.category.SETUP_WIZARD \
    -p "$PACKAGE" 2>/dev/null | tail -n 1)"

case "$ACTIVITY" in
    "$PACKAGE"/*) ;;
    *)
        ACTIVITY="$(cmd package resolve-activity --brief --user 0 \
            -a android.intent.action.MAIN \
            -c android.intent.category.LAUNCHER \
            -p "$PACKAGE" 2>/dev/null | tail -n 1)"
        ;;
esac

case "$ACTIVITY" in
    "$PACKAGE"/*)
        log -t "$TAG" "Launching first-boot onboarding: $ACTIVITY"
        if am start -W -n "$ACTIVITY" >/dev/null 2>&1; then
            log -t "$TAG" "Onboarding launched. Completion is recorded by the setup app, not by this launcher."
            exit 0
        fi
        log -t "$TAG" "Onboarding launch failed; setup remains incomplete."
        exit 1
        ;;
    *)
        log -t "$TAG" "No valid setup activity found for $PACKAGE; setup remains incomplete."
        exit 1
        ;;
esac
