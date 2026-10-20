#!/system/bin/sh
# Lucid Desktop: Android freeform + Ubuntu chroot, with a continuously synced app menu.
# This launcher is intentionally conservative: it never exposes TCP ADB unless
# IPv4 and IPv6 non-loopback traffic is blocked, and never marks partial setup complete.
set -u

TAG="LucidDesktop"
CHROOT_DIR="/data/local/lucid_desktop"
TAR_PATH="/system/usr/lucid/ubuntu-base-arm64.tar.gz"
INIT_MARKER="$CHROOT_DIR/.lucid_initialized"
SYNC_PID_FILE="$CHROOT_DIR/.lucid-app-sync.pid"
BRIDGE_PORT=5555
ANDROID_UID="$(id -u)"
ANDROID_GID="$(id -g)"

log_msg() { log -t "$TAG" "$*"; }
is_mounted() { awk -v target="$1" '$2 == target { found=1 } END { exit !found }' /proc/mounts; }

bind_once() {
    source_path="$1"
    target_path="$2"
    mkdir -p "$target_path"
    if is_mounted "$target_path"; then return 0; fi
    mount -o bind,nosuid,nodev "$source_path" "$target_path"
}

protect_adb_listener() {
    for firewall in iptables ip6tables; do
        if ! command -v "$firewall" >/dev/null 2>&1; then
            log_msg "$firewall unavailable; TCP ADB remains disabled."
            return 1
        fi
        if ! "$firewall" -C INPUT -p tcp --dport "$BRIDGE_PORT" ! -i lo -j DROP >/dev/null 2>&1; then
            "$firewall" -I INPUT 1 -p tcp --dport "$BRIDGE_PORT" ! -i lo -j DROP || {
                log_msg "Could not install $firewall protection; TCP ADB remains disabled."
                return 1
            }
        fi
    done
    return 0
}

start_local_adbd() {
    protect_adb_listener || return 1
    setprop service.adb.tcp.port "$BRIDGE_PORT"
    stop adbd
    start adbd
    sleep 2
    return 0
}

cleanup_mounts() {
    for target in "$CHROOT_DIR/sys" "$CHROOT_DIR/proc" "$CHROOT_DIR/dev"; do
        if is_mounted "$target"; then
            umount "$target" 2>/dev/null || log_msg "Could not unmount $target."
        fi
    done
}
stop_app_sync() {
    if [ -s "$SYNC_PID_FILE" ]; then
        old_pid="$(cat "$SYNC_PID_FILE" 2>/dev/null)"
        case "$old_pid" in ''|*[!0-9]*) ;; *) kill "$old_pid" 2>/dev/null || true ;; esac
        rm -f "$SYNC_PID_FILE"
    fi
}
trap cleanup_mounts EXIT
trap 'stop_app_sync; cleanup_mounts; exit 143' TERM
trap 'stop_app_sync; cleanup_mounts; exit 130' INT

# Enable Android's resizable/freeform activities; this is DeX-like windowing,
# not a replacement for OEM desktop-mode support.
settings put global enable_freeform_support 1
settings put global force_resizable_activities 1

mkdir -p "$CHROOT_DIR"
if [ ! -x "$CHROOT_DIR/bin/bash" ]; then
    if [ ! -s "$TAR_PATH" ]; then
        log_msg "Ubuntu base archive missing: $TAR_PATH"
        exit 1
    fi
    log_msg "Extracting Ubuntu base; this can take several minutes."
    if ! tar -xf "$TAR_PATH" -C "$CHROOT_DIR"; then
        log_msg "Ubuntu extraction failed."
        exit 1
    fi
fi

bind_once /dev "$CHROOT_DIR/dev" || exit 1
bind_once /proc "$CHROOT_DIR/proc" || exit 1
bind_once /sys "$CHROOT_DIR/sys" || exit 1
mkdir -p "$CHROOT_DIR/etc"
if [ -w "$CHROOT_DIR/etc/resolv.conf" ] || [ -d "$CHROOT_DIR/etc" ]; then
    printf 'nameserver 1.1.1.1\nnameserver 8.8.8.8\n' > "$CHROOT_DIR/etc/resolv.conf" 2>/dev/null || log_msg "Could not update chroot DNS."
fi

# Provisioning uses a real completion marker; a failed apt/theme run will retry.
if [ ! -f "$INIT_MARKER" ]; then
    cmd notification post -S default -t "LucidOS" "desktop-setup" "Preparing Lucid Desktop. First boot needs network access."
    chroot "$CHROOT_DIR" /bin/bash <<'CHROOT_SETUP'
set -e
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y xfce4 xfce4-goodies plank dbus-x11 adb wget curl git desktop-file-utils hicolor-icon-theme
# WhiteSur gives the GNOME/macOS visual direction; XFCE keeps this usable on ARM64.
if [ ! -d /opt/WhiteSur-gtk-theme/.git ]; then
    git clone --depth 1 https://github.com/vinceliuice/WhiteSur-gtk-theme.git /opt/WhiteSur-gtk-theme
fi
/opt/WhiteSur-gtk-theme/install.sh -c Dark -t default -N glassy
if [ ! -d /opt/WhiteSur-icon-theme/.git ]; then
    git clone --depth 1 https://github.com/vinceliuice/WhiteSur-icon-theme.git /opt/WhiteSur-icon-theme
fi
/opt/WhiteSur-icon-theme/install.sh
mkdir -p /usr/local/bin /usr/local/share/lucid /root/.config/autostart /root/Desktop /root/.local/share/applications
cat >/usr/local/bin/lucid-android-app <<'ANDROID_APP'
#!/bin/bash
set -u
PKG="${1:-}"
[[ "$PKG" =~ ^[A-Za-z0-9_.]+$ ]] || { echo "Usage: lucid-android-app package.name" >&2; exit 2; }
adb connect 127.0.0.1:5555 >/dev/null 2>&1 || true
SERIAL=127.0.0.1:5555
adb -s "$SERIAL" get-state >/dev/null 2>&1 || { notify-send "Lucid Desktop" "Android bridge is offline"; exit 1; }
COMPONENT="$(adb -s "$SERIAL" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER "$PKG" 2>/dev/null | tr -d '\r' | tail -n 1)"
if [[ "$COMPONENT" != */* ]]; then
  COMPONENT="$(adb -s "$SERIAL" shell cmd package resolve-activity --brief "$PKG" 2>/dev/null | tr -d '\r' | tail -n 1)"
fi
[[ "$COMPONENT" == */* ]] || { notify-send "Lucid Desktop" "No launchable activity found for $PKG"; exit 1; }
adb -s "$SERIAL" shell am start --windowingMode 5 -n "$COMPONENT"
ANDROID_APP
chmod 0755 /usr/local/bin/lucid-android-app

cat >/usr/local/bin/lucid-sync-android-apps <<'SYNC_APPS'
#!/bin/bash
set -u
OUT=/root/.local/share/applications
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
adb connect 127.0.0.1:5555 >/dev/null 2>&1 || true
adb -s 127.0.0.1:5555 get-state >/dev/null 2>&1 || exit 0
adb -s 127.0.0.1:5555 shell pm list packages -3 2>/dev/null | tr -d '\r' | sed 's/^package://' | sort -u >"$TMP/packages"
while IFS= read -r pkg; do
  [[ "$pkg" =~ ^[A-Za-z0-9_.]+$ ]] || continue
  file="$OUT/lucid-android-$pkg.desktop"
  printf '[Desktop Entry]\nType=Application\nName=%s (Android)\nComment=Launch Android app in a Lucid freeform window\nExec=/usr/local/bin/lucid-android-app %s\nIcon=phone\nTerminal=false\nCategories=Network;Utility;\nStartupNotify=true\n' "$pkg" "$pkg" >"$TMP/$pkg.desktop"
  if [ ! -f "$file" ] || ! cmp -s "$TMP/$pkg.desktop" "$file"; then install -m 0644 "$TMP/$pkg.desktop" "$file"; fi
done <"$TMP/packages"
# Remove only stale shortcuts created by this bridge.
find "$OUT" -maxdepth 1 -type f -name 'lucid-android-*.desktop' -print0 | while IFS= read -r -d '' file; do
  pkg="${file##*/lucid-android-}"
  pkg="${pkg%.desktop}"
  grep -Fxq "$pkg" "$TMP/packages" || rm -f "$file"
done
SYNC_APPS
chmod 0755 /usr/local/bin/lucid-sync-android-apps

# Termux:Widget reads executable scripts from ~/.shortcuts. If Termux exists,
# publish launchers there so Linux GUI apps can be launched from Android's app
# drawer via the Termux:Widget shortcut surface. Root is required for chroot.
sync_termux_shortcuts() {
    TERMUX_DATA="/data/data/com.termux"
    [ -d "$TERMUX_DATA" ] || return 0
    TERMUX_UID="$(stat -c '%u' "$TERMUX_DATA" 2>/dev/null)"
    TERMUX_GID="$(stat -c '%g' "$TERMUX_DATA" 2>/dev/null)"
    case "$TERMUX_UID:$TERMUX_GID" in *[!0-9:]*|'':*) return 1 ;; esac
    SHORTCUTS="$TERMUX_DATA/files/home/.shortcuts/lucid-linux"
    mkdir -p "$SHORTCUTS" || return 1
    chown "$TERMUX_UID:$TERMUX_GID" "$TERMUX_DATA/files/home/.shortcuts" "$SHORTCUTS" 2>/dev/null || true
    find "$CHROOT_DIR/usr/share/applications" "$CHROOT_DIR/root/.local/share/applications" -maxdepth 1 -type f -name '*.desktop' 2>/dev/null | while IFS= read -r desktop; do
        app_id="${desktop##*/}"
        app_id="${app_id%.desktop}"
        case "$app_id" in *[!A-Za-z0-9_.-]*|'') continue ;; esac
        title="$(sed -n 's/^Name=//p' "$desktop" | head -n 1 | tr -cd 'A-Za-z0-9 ._()-' | cut -c1-60)"
        [ -n "$title" ] || title="$app_id"
        shortcut="$SHORTCUTS/$app_id"
        cat >"$shortcut" <<EOF_SHORTCUT
#!/system/bin/sh
su -c '/system/bin/lucid-linux-app "$app_id"'
EOF_SHORTCUT
        chmod 0755 "$shortcut"
        chown "$TERMUX_UID:$TERMUX_GID" "$shortcut" 2>/dev/null || true
    done
    # Termux:Widget must be installed for these scripts to appear as launchable shortcuts.
    log_msg "Synced Linux desktop app launchers to Termux shortcuts; Termux:Widget is required to expose them in Android."
}

# Keep a native app menu and panel rather than relying on one-time generated icons.
mkdir -p /root/.config/xfce4/xfconf/xfce-perchannel-xml /root/.config/autostart
cat >/root/.config/xfce4/xfconf/xfce-perchannel-xml/xsettings.xml <<'XSETTINGS'
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xsettings" version="1.0">
  <property name="Net" type="empty">
    <property name="ThemeName" type="string" value="WhiteSur-Dark"/>
    <property name="IconThemeName" type="string" value="WhiteSur"/>
    <property name="CursorThemeName" type="string" value="WhiteSur-cursors"/>
  </property>
</channel>
XSETTINGS
cat >/root/.config/autostart/lucid-app-sync.desktop <<'AUTOSYNC'
[Desktop Entry]
Type=Application
Name=Lucid Android App Sync
Exec=/usr/local/bin/lucid-sync-android-apps
Terminal=false
OnlyShowIn=XFCE;
AUTOSYNC
# WhiteSur installs can vary by release; use a reliable dark base when the exact theme name differs.
xfconf-query -c xsettings -p /Net/ThemeName -s WhiteSur-Dark --create -t string 2>/dev/null || true
xfconf-query -c xsettings -p /Net/IconThemeName -s WhiteSur --create -t string 2>/dev/null || true
xfconf-query -c xfwm4 -p /general/theme -s WhiteSur-Dark --create -t string 2>/dev/null || true
CHROOT_SETUP
    SETUP_STATUS=$?
    if [ "$SETUP_STATUS" -ne 0 ]; then
        log_msg "Desktop provisioning failed (exit $SETUP_STATUS); will retry next activation."
        exit "$SETUP_STATUS"
    fi
    touch "$INIT_MARKER" || { log_msg "Could not write setup marker."; exit 1; }
    cmd notification post -S default -t "LucidOS" "desktop-ready" "Lucid Desktop initialized."
fi

# Start Android bridge only with loopback-only firewall protection.
sync_termux_shortcuts || log_msg "Could not publish Termux Linux-app shortcuts."
if start_local_adbd; then
    chroot "$CHROOT_DIR" /usr/local/bin/lucid-sync-android-apps >/dev/null 2>&1 || true
else
    log_msg "Android app mirroring unavailable; Linux desktop can still run."
fi

# A low-frequency background sync catches installs/uninstalls after boot.
(
    echo $$ > "$SYNC_PID_FILE"
    while true; do
        sleep 60
        sync_termux_shortcuts || true
        if protect_adb_listener; then
            chroot "$CHROOT_DIR" /usr/local/bin/lucid-sync-android-apps >/dev/null 2>&1 || true
        fi
    done
) &
SYNC_PID=$!
echo "$SYNC_PID" > "$SYNC_PID_FILE"

# Start Termux:X11 and the desktop session.
am start -n com.termux.x11/com.termux.x11.MainActivity >/dev/null 2>&1 || log_msg "Termux:X11 launch failed; install/enable Termux:X11 to display the Linux desktop."
chroot "$CHROOT_DIR" /bin/bash -lc 'export DISPLAY=:0; export XDG_RUNTIME_DIR=/run/user/0; mkdir -p "$XDG_RUNTIME_DIR"; chmod 0700 "$XDG_RUNTIME_DIR"; exec startxfce4' 
