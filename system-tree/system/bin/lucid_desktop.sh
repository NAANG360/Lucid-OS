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

# A root-off preference is authoritative even if casting is requested.
# The Android init property trigger also stops this service when switched off.
if [ "$(getprop persist.lucid.chroot_root)" != "1" ]; then
    log -t "LucidDesktop" "Desktop not started: chroot root access is disabled in Developer Options."
    exit 1
fi

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
    # Unmount nested mounts before their parent; otherwise /dev can stay busy.
    for target in "$CHROOT_DIR/dev/pts" "$CHROOT_DIR/sys" "$CHROOT_DIR/proc" "$CHROOT_DIR/dev"; do
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
# A plain bind of /dev does not reliably carry the devpts submount into chroot.
mkdir -p "$CHROOT_DIR/dev/pts"
bind_once /dev/pts "$CHROOT_DIR/dev/pts" || { log_msg "Could not bind /dev/pts; PTY-based apps may fail."; exit 1; }
bind_once /proc "$CHROOT_DIR/proc" || exit 1
bind_once /sys "$CHROOT_DIR/sys" || exit 1
mkdir -p "$CHROOT_DIR/etc"
# Avoid writing through a resolv.conf symlink into a host/system path.
if [ -L "$CHROOT_DIR/etc/resolv.conf" ]; then rm -f "$CHROOT_DIR/etc/resolv.conf"; fi
if [ ! -e "$CHROOT_DIR/etc/resolv.conf" ] || [ -w "$CHROOT_DIR/etc/resolv.conf" ]; then
    printf 'nameserver 1.1.1.1\nnameserver 8.8.8.8\n' > "$CHROOT_DIR/etc/resolv.conf" 2>/dev/null || log_msg "Could not update chroot DNS."
fi

# Provisioning uses a real completion marker; a failed apt/theme run will retry.
if [ ! -f "$INIT_MARKER" ]; then
    cmd notification post -S default -t "LucidOS" "desktop-setup" "Preparing Lucid Desktop. First boot needs network access."
    chroot "$CHROOT_DIR" /bin/bash <<'CHROOT_SETUP'
set -e
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y xfce4 xfce4-goodies plank dbus-x11 adb desktop-file-utils hicolor-icon-theme libnotify-bin adwaita-icon-theme
# Lucid theme is shipped inline: no GitHub clone, installer, or Termux required.
mkdir -p /usr/share/themes/Lucid-Dark/gtk-3.0 /usr/share/themes/Lucid-Dark/gtk-4.0
cat >/usr/share/themes/Lucid-Dark/gtk-3.0/gtk.css <<'LUCID_GTK'
@define-color theme_bg_color #10131d;
@define-color theme_fg_color #e9edff;
@define-color theme_base_color #171b29;
@define-color theme_text_color #e9edff;
@define-color theme_selected_bg_color #8b5cf6;
@define-color theme_selected_fg_color #ffffff;
@define-color accent_color #8b5cf6;
@define-color accent_bg_color #8b5cf6;
@define-color accent_fg_color #ffffff;
window, .background { background-color: #10131d; color: #e9edff; }
headerbar, .titlebar { background: #191e2d; color: #f5f6ff; border: 0; box-shadow: none; }
button { background: #252b40; color: #e9edff; border: 1px solid #343c58; border-radius: 10px; padding: 6px 12px; }
button:hover { background: #34304f; border-color: #8b5cf6; }
button:checked, button:active { background: #6d48c7; color: #fff; }
entry, textview, spinbutton, combobox, .view { background: #171b29; color: #e9edff; border-color: #343c58; }
selection { background: #8b5cf6; color: #fff; }
menu, popover, .popover { background: #191e2d; color: #e9edff; border: 1px solid #343c58; }
tooltip { background: #242b40; color: #fff; border: 1px solid #8b5cf6; }
scrollbar slider { background: #555d7c; border-radius: 8px; }
LUCID_GTK
cat >/usr/share/themes/Lucid-Dark/gtk-4.0/gtk.css <<'LUCID_GTK4'
@import url("resource:///org/gtk/libgtk/theme/Adwaita/gtk.css");
:root { --window-bg-color: #10131d; --window-fg-color: #e9edff; --view-bg-color: #171b29; --view-fg-color: #e9edff; --headerbar-bg-color: #191e2d; --headerbar-fg-color: #f5f6ff; --accent-bg-color: #8b5cf6; --accent-fg-color: #ffffff; --accent-color: #a78bfa; }
LUCID_GTK4
mkdir -p /usr/share/icons/Lucid
cat >/usr/share/icons/Lucid/index.theme <<'LUCID_ICONS'
[Icon Theme]
Name=Lucid
Comment=LucidOS symbolic icon theme with Adwaita fallback
Inherits=Adwaita,hicolor
Directories=
LUCID_ICONS
mkdir -p /usr/local/bin /usr/local/share/lucid /root/.config/autostart /root/Desktop /root/.local/share/applications
cat >/usr/local/bin/lucid-android-app <<'ANDROID_APP'
#!/bin/bash
set -u
PKG="${1:-}"
[[ "$PKG" =~ ^[A-Za-z0-9_.]+$ ]] || { echo "Usage: lucid-android-app package.name" >&2; exit 2; }
adb connect 127.0.0.1:5555 >/dev/null 2>&1 || true
SERIAL=127.0.0.1:5555
adb -s "$SERIAL" get-state >/dev/null 2>&1 || { notify-send "Lucid Desktop" "Android bridge is offline"; exit 1; }
COMPONENT="$(adb -s "$SERIAL" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p "$PKG" 2>/dev/null | tr -d '\r' | tail -n 1)"
if [[ "$COMPONENT" != */* ]]; then
  COMPONENT="$(adb -s "$SERIAL" shell cmd package resolve-activity --brief -p "$PKG" 2>/dev/null | tr -d '\r' | tail -n 1)"
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
adb -s 127.0.0.1:5555 shell pm list packages 2>/dev/null | tr -d '\r' | sed 's/^package://' | sort -u >"$TMP/packages"
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

# Linux apps are launched from the Linux desktop itself. Android-side launching
# is provided by the Lucid bridge when installed; Termux/Termux:Widget is optional.
# Keep a native app menu and panel rather than relying on one-time generated icons.
mkdir -p /root/.config/xfce4/xfconf/xfce-perchannel-xml /root/.config/autostart
cat >/root/.config/xfce4/xfconf/xfce-perchannel-xml/xsettings.xml <<'XSETTINGS'
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xsettings" version="1.0">
  <property name="Net" type="empty">
    <property name="ThemeName" type="string" value="Lucid-Dark"/>
    <property name="IconThemeName" type="string" value="Lucid"/>
    <property name="CursorThemeName" type="string" value="Adwaita"/>
  </property>
</channel>
XSETTINGS
cat >/root/.config/autostart/lucid-plank.desktop <<'PLANK'
[Desktop Entry]
Type=Application
Name=Lucid Dock
Exec=plank
Terminal=false
OnlyShowIn=XFCE;
X-GNOME-Autostart-enabled=true
PLANK
cat >/root/.config/autostart/lucid-app-sync.desktop <<'AUTOSYNC'
[Desktop Entry]
Type=Application
Name=Lucid Android App Sync
Exec=/usr/local/bin/lucid-sync-android-apps
Terminal=false
OnlyShowIn=XFCE;
AUTOSYNC
# Lucid installs can vary by release; use a reliable dark base when the exact theme name differs.
xfconf-query -c xsettings -p /Net/ThemeName -s Lucid-Dark --create -t string 2>/dev/null || true
xfconf-query -c xsettings -p /Net/IconThemeName -s Lucid --create -t string 2>/dev/null || true
xfconf-query -c xfwm4 -p /general/theme -s Lucid-Dark --create -t string 2>/dev/null || true
# Put the Plank dock at the bottom, with a clean macOS-like icon zoom.
mkdir -p /root/.config/plank/dock1
cat >/root/.config/plank/dock1/settings <<'PLANK_SETTINGS'
[PlankDockItemPreferences]
PinnedOnly=false
ShowDockItem=false
LockItems=true
IconSize=48
ZoomEnabled=true
ZoomPercent=140
Alignment=3
ItemsAlignment=3
HideMode=1
Position=bottom
Theme=Gtk+
PLANK_SETTINGS
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
if start_local_adbd; then
    chroot "$CHROOT_DIR" /usr/local/bin/lucid-sync-android-apps >/dev/null 2>&1 || true
else
    log_msg "Android app mirroring unavailable; Linux desktop can still run."
fi

# A low-frequency background sync catches installs/uninstalls after boot.
(
    while true; do
        sleep 60
        if protect_adb_listener; then
            chroot "$CHROOT_DIR" /usr/local/bin/lucid-sync-android-apps >/dev/null 2>&1 || true
        fi
    done
) &
SYNC_PID=$!
echo "$SYNC_PID" > "$SYNC_PID_FILE"

# Start Termux:X11 and the desktop session.
# A native Android X server is required to remove the final Termux:X11 dependency.
# Do not silently claim desktop mode is available if no display server is present.
if ! pm path com.termux.x11 >/dev/null 2>&1; then
    log_msg "No compatible X11 display provider installed. Chroot is prepared, but GUI session cannot be displayed."
    exit 1
fi
am start -n com.termux.x11/com.termux.x11.MainActivity >/dev/null 2>&1 || log_msg "X11 display provider launch failed."
# startxfce4 expects a session bus; dbus-x11 is installed during provisioning.
# Keep the session in the foreground so init can stop/restart the desktop service.
chroot "$CHROOT_DIR" /bin/bash -lc 'export DISPLAY="${DISPLAY:-:0}"; export XDG_RUNTIME_DIR=/run/user/0; mkdir -p "$XDG_RUNTIME_DIR"; chmod 0700 "$XDG_RUNTIME_DIR"; exec dbus-launch --exit-with-session startxfce4' 
