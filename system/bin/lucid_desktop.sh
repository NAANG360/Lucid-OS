#!/system/bin/sh
# Lucid Desktop controller. Requires a ROM-integrated root provider and Developer Options policy.
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
[ "$(id -u 2>/dev/null)" = "0" ] || fail "Root permission is unavailable. A ROM-integrated root provider is required."
case "${1:-check}" in
 check)
   [ -d "$ROOTFS" ] && [ -x "$ROOTFS/bin/bash" ] && ROOTFS_STATE=ready || ROOTFS_STATE=missing
   [ -x "$ROOTFS/usr/bin/startxfce4" ] && XFCE_STATE=ready || XFCE_STATE=missing
   [ -f "$ARCHIVE" ] && ARCHIVE_STATE=ready || ARCHIVE_STATE=missing
   command -v chroot >/dev/null 2>&1 && CHROOT_STATE=ready || CHROOT_STATE=missing
   [ -d /data/data/com.termux/files/usr ] && TERMUX_STATE=ready || TERMUX_STATE=missing
   [ -S /data/data/com.termux/files/usr/tmp/.X11-unix/X0 ] && X11_STATE=socket-ready || X11_STATE=not-detected
   echo "Lucid Desktop diagnostics:"
   echo " rootfs: $ROOTFS_STATE"
   echo " XFCE: $XFCE_STATE"
   echo " archive: $ARCHIVE_STATE"
   echo " chroot: $CHROOT_STATE"
   echo " Termux: $TERMUX_STATE"
   echo " X11 socket: $X11_STATE (launch the X server before starting)"
   echo " root toggle: $(getprop persist.lucid.desktop_root 2>/dev/null)"
   echo " log: $LOG"
   exit 0 ;;
 stop)
   if [ -f "$PIDFILE" ]; then
     P="$(cat "$PIDFILE" 2>/dev/null)"
     case "$P" in ''|*[!0-9]*) log "Ignoring invalid PID file";; *) kill "$P" 2>/dev/null || true;; esac
     rm -f "$PIDFILE"
   fi
   # Do not pkill process-name patterns: that can terminate unrelated desktop sessions.
   if [ -f /data/local/tmp/lucid-desktop-session.pid ]; then
     P="$(cat /data/local/tmp/lucid-desktop-session.pid 2>/dev/null)"
     case "$P" in ''|*[!0-9]*) log "Ignoring invalid session PID";; *) kill "$P" 2>/dev/null || true;; esac
     rm -f /data/local/tmp/lucid-desktop-session.pid
   fi
   echo "LUCID_DESKTOP_STOPPED"
   exit 0 ;;
 start) ;;
 *) fail "Usage: lucid_desktop.sh {check|start|stop}" ;;
esac
[ "$(getprop persist.lucid.desktop_root 2>/dev/null)" = "1" ] || fail "Lucid Desktop root is disabled in Developer Options. Enable it and grant root access through the ROM root provider."
[ -x /system/bin/chroot ] || fail "/system/bin/chroot is missing"
if [ ! -x "$ROOTFS/bin/bash" ]; then
  [ -f "$ARCHIVE" ] || fail "Ubuntu rootfs archive not found at $ARCHIVE"
  mkdir -p "$ROOTFS" || fail "Cannot create rootfs directory"
  tar -xpf "$ARCHIVE" -C "$ROOTFS" >>"$LOG" 2>&1 || fail "Rootfs extraction failed; see $LOG"
fi
[ -x "$ROOTFS/bin/bash" ] || fail "Rootfs incomplete: /bin/bash missing after extraction"
mkdir -p "$ROOTFS/dev" "$ROOTFS/proc" "$ROOTFS/sys" "$ROOTFS/tmp" "$ROOTFS/run" "$ROOTFS/run/dbus" "$ROOTFS/tmp/runtime-root"
# Android toybox does not consistently provide mountpoint; inspect /proc/mounts instead.
is_mounted(){ awk -v p="$1" '$2==p {found=1} END{exit !found}' /proc/mounts; }
is_mounted "$ROOTFS/dev" || mount --bind /dev "$ROOTFS/dev" || fail "Could not bind /dev"
is_mounted "$ROOTFS/proc" || mount -t proc proc "$ROOTFS/proc" || fail "Could not mount /proc"
is_mounted "$ROOTFS/sys" || mount --bind /sys "$ROOTFS/sys" || fail "Could not bind /sys"
chmod 700 "$ROOTFS/tmp/runtime-root" 2>/dev/null || true
mkdir -p "$ROOTFS/etc"
if [ ! -s "$ROOTFS/etc/resolv.conf" ]; then
  printf 'nameserver 1.1.1.1\nnameserver 8.8.8.8\n' > "$ROOTFS/etc/resolv.conf"
fi
[ -x "$ROOTFS/usr/bin/startxfce4" ] || fail "XFCE is not installed in the rootfs. Startup will not attempt apt-get."
[ -d /data/data/com.termux/files/usr ] || fail "Termux is not installed. Install Termux and a compatible X11 server."
# Termux:X11 must be launched manually first; never report success if no display socket is present.
XSOCKET=/data/data/com.termux/files/usr/tmp/.X11-unix/X0
[ -S "$XSOCKET" ] || fail "X11 display :0 is not listening. Open Termux:X11, start its display, then retry."
if [ -x "$ROOTFS/usr/bin/dbus-run-session" ]; then
  SESSION="/usr/bin/dbus-run-session -- /usr/bin/startxfce4"
elif [ -x "$ROOTFS/usr/bin/dbus-launch" ]; then
  SESSION="/usr/bin/dbus-launch --exit-with-session /usr/bin/startxfce4"
else
  SESSION="/usr/bin/startxfce4"
fi
log "Starting XFCE on DISPLAY=$DISPLAY_NUM"
(
  export DISPLAY="$DISPLAY_NUM" HOME=/root USER=root LOGNAME=root XDG_RUNTIME_DIR=/tmp/runtime-root
  /system/bin/chroot "$ROOTFS" /usr/bin/env DISPLAY="$DISPLAY_NUM" HOME=/root USER=root LOGNAME=root XDG_RUNTIME_DIR=/tmp/runtime-root /bin/sh -c "$SESSION" >>"$LOG" 2>&1
  RC=$?
  echo "Desktop session exited with code $RC" >>"$LOG"
  rm -f /data/local/tmp/lucid-desktop-session.pid
) &
echo $! > /data/local/tmp/lucid-desktop-session.pid
echo $! > "$PIDFILE"
echo "LUCID_DESKTOP_STARTED"
exit 0
