#!/system/bin/sh
# LucidOS root status check. Never manufacture root by chmod/remounting /system.
TAG="LucidRoot"
MAGISK=""
for candidate in /data/adb/magisk/magisk /system/bin/magisk /sbin/magisk; do
    if [ -x "$candidate" ]; then MAGISK="$candidate"; break; fi
done
if [ -n "$MAGISK" ]; then
    VERSION="$("$MAGISK" -v 2>/dev/null | head -n 1)"
    log -t "$TAG" "Magisk detected ($VERSION); root grants are managed by Magisk."
    setprop sys.lucid.root_available 1
    exit 0
fi
if [ -x /system/xbin/su ] || [ -x /system/bin/su ]; then
    log -t "$TAG" "su executable exists but no supported root manager was detected; refusing chmod/remount."
else
    log -t "$TAG" "No root manager detected. A compatible boot image must be patched before root is available."
fi
setprop sys.lucid.root_available 0
exit 1
