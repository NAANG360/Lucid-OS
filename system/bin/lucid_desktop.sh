#!/system/bin/sh
# Lucid Desktop controller. Install mode 0755; invoked by the privileged ROM controller.
set -u
ROOTFS=/data/local/lucid_desktop
ARCHIVE=/system/usr/lucid/ubuntu-base-arm64.tar.gz
LOG=/data/local/tmp/lucid-desktop.log
PIDFILE=/data/local/tmp/lucid-desktop.pid
DISPLAY_NUM=:0
mkdir -p /data/local/tmp
exec 3>>"$LOG"
log(){ echo "[$(date 2>/dev/null)] $*" >&3; }
fail(){ log "ERROR: $*"; echo "LUCID_DESKTOP_ERROR: $*"; exit 1; }
[ "$(id -u 2>/dev/null)" = "0" ] || fail "Root permission is unavailable. Enable the ROM root provider for Lucid Desktop."
case "${1:-check}" in
 check)
   [ -d "$ROOTFS" ] && [ -x "$ROOTFS/bin/bash" ] && log "rootfs present" || log "rootfs missing or incomplete"
   [ -x "$ROOTFS/usr/bin/startxfce4" ] && log "XFCE launcher present" || log "XFCE not installed in rootfs"
   [ -f "$ARCHIVE" ] && log "rootfs archive present" || log "rootfs archive missing"
   command -v chroot >/dev/null 2>&1 && log "chroot available" || log "chroot command missing"
   [ -d /dev ] && [ -d /proc ] && [ -d /sys ] && log "Android mount points available"
   [ -d /data/data/com.termux/files/usr/bin ] && log "Termux installed" || log "Termux not found"
   echo "Lucid Desktop diagnostics:"
   echo " rootfs: $([ -x "$ROOTFS/bin/bash" ] && echo ready || echo missing)"
   echo " XFCE: $([ -x "$ROOTFS/usr/bin/startxfce4" ] && echo ready || echo missing)"
   echo " chroot: $(command -v chroot 2>/dev/null || echo missing)"
   echo " X11: install and launch a compatible X server (Termux:X11 is the supported backend)"
   echo " log: $LOG"
   exit 0 ;;
 stop)
   if [ -f "$PIDFILE" ]; then
     P="$(cat "$PIDFILE" 2>/dev/null)"
     [ -n "$P" ] && kill "$P" 2>/dev/null
     rm -f "$PIDFILE"
   fi
   pkill -f 'startxfce4|xfce4-session|xfwm4|xfce4-panel' 2>/dev/null
   echo "LUCID_DESKTOP_STOPPED"
   exit 0 ;;
 start) ;;
 *) fail "Usage: lucid_desktop.sh {check|start|stop}" ;;
esac
[ -x /system/bin/chroot ] || fail "/system/bin/chroot is missing"
if [ ! -x "$ROOTFS/bin/bash" ]; then
  [ -f "$ARCHIVE" ] || fail "Ubuntu rootfs archive not found at $ARCHIVE"
  mkdir -p "$ROOTFS" || fail "Cannot create rootfs directory"
  tar -xpf "$ARCHIVE" -C "$ROOTFS" >>"$LOG" 2>&1 || fail "Rootfs extraction failed; see $LOG"
fi
[ -x "$ROOTFS/bin/bash" ] || fail "Rootfs incomplete: /bin/bash missing after extraction"
mkdir -p "$ROOTFS/dev" "$ROOTFS/proc" "$ROOTFS/sys" "$ROOTFS/tmp" "$ROOTFS/run" "$ROOTFS/run/dbus"
mountpoint -q "$ROOTFS/dev" 2>/dev/null || mount --bind /dev "$ROOTFS/dev" || fail "Could not bind /dev"
mountpoint -q "$ROOTFS/proc" 2>/dev/null || mount -t proc proc "$ROOTFS/proc" || fail "Could not mount /proc"
mountpoint -q "$ROOTFS/sys" 2>/dev/null || mount --bind /sys "$ROOTFS/sys" || fail "Could not bind /sys"
# DNS is seeded only when empty; avoid the previous empty resolv.conf failure.
mkdir -p "$ROOTFS/etc"
if [ ! -s "$ROOTFS/etc/resolv.conf" ]; then
  printf 'nameserver 1.1.1.1\nnameserver 8.8.8.8\n' > "$ROOTFS/etc/resolv.conf"
fi
# Verify X11 server is listening before starting clients. Termux:X11 uses :0 by default.
if [ -x /data/data/com.termux/files/usr/bin/termux-x11 ]; then
  export DISPLAY="$DISPLAY_NUM"
elif [ -d /data/data/com.termux/files/usr ]; then
  export DISPLAY="$DISPLAY_NUM"
else
  fail "Termux not installed; install Termux and Termux:X11, launch the X11 app, then retry"
fi
if [ ! -x "$ROOTFS/usr/bin/startxfce4" ]; then
  fail "XFCE is not installed in the rootfs. Install packages once in a supported root-capable environment; runtime startup will not attempt apt-get."
fi
# Avoid dbus-launch assumption; dbus-run-session is preferred when available.
if [ -x "$ROOTFS/usr/bin/dbus-run-session" ]; then
  SESSION="/usr/bin/dbus-run-session -- /usr/bin/startxfce4"
elif [ -x "$ROOTFS/usr/bin/dbus-launch" ]; then
  SESSION="/usr/bin/dbus-launch --exit-with-session /usr/bin/startxfce4"
else
  SESSION="/usr/bin/startxfce4"
fi
log "Starting desktop with DISPLAY=$DISPLAY"
echo "LUCID_DESKTOP_STARTED"
# Run in background so the Android app remains responsive.
(
  export DISPLAY="$DISPLAY_NUM"
  export HOME=/root
  export USER=root
  export LOGNAME=root
  export XDG_RUNTIME_DIR=/tmp/runtime-root
  mkdir -p "$ROOTFS/tmp/runtime-root"
  chmod 700 "$ROOTFS/tmp/runtime-root"
  /system/bin/chroot "$ROOTFS" /usr/bin/env DISPLAY="$DISPLAY_NUM" HOME=/root USER=root LOGNAME=root XDG_RUNTIME_DIR=/tmp/runtime-root /bin/sh -c "$SESSION" >>"$LOG" 2>&1
  echo "Desktop session exited with code $?" >>"$LOG"
) &
echo $! > "$PIDFILE"
exit 0