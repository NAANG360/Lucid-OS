#!/system/bin/sh

MARKER="/data/system/lucid_setup_complete"

# Setup has already completed.
if [ -f "$MARKER" ]; then
    exit 0
fi

# Wait for the framework/package manager to settle.
sleep 8

# Launch LucidOS first-boot setup.
am start \
    -a android.intent.action.MAIN \
    -c android.intent.category.SETUP_WIZARD \
    -n com.cosmos.setup/com.cosmos.setup.MainActivity

exit 0
