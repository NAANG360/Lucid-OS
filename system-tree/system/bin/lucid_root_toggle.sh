#!/system/bin/sh
STATUS=$(getprop persist.lucid.root)

if [ "$STATUS" = "1" ]; then
    chmod 0755 /system/xbin/su
else
    chmod 0000 /system/xbin/su
fi
