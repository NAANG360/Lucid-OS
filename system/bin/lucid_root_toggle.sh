#!/system/bin/sh
# Explicit ROM root toggle for Lucid Desktop; this does not exploit or bypass Android.
set -u
PROP=persist.lucid.desktop_root
case "${1:-status}" in
 enable)
   setprop "$PROP" 1
   echo "Lucid Desktop root requested. The ROM's configured su provider must authorize the app."
   ;;
 disable)
   setprop "$PROP" 0
   # Stop the desktop so existing root sessions do not outlive the toggle.
   /system/bin/lucid_desktop.sh stop >/dev/null 2>&1
   echo "Lucid Desktop root disabled for future starts; existing privileged providers may maintain separate policy."
   ;;
 status)
   [ "$(getprop "$PROP")" = "1" ] && echo enabled || echo disabled
   ;;
 *) echo "Usage: lucid_root_toggle.sh {enable|disable|status}"; exit 2 ;;
esac