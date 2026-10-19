#!/system/bin/sh
set -u

TAG="LucidDesktop"
CHROOT_DIR="/data/local/lucid_desktop"
TAR_PATH="/system/usr/lucid/ubuntu-base-arm64.tar.gz"
INIT_MARKER="$CHROOT_DIR/.lucid_initialized"

bind_once() {
    source_path="$1"
    target_path="$2"
    mkdir -p "$target_path"
    if awk -v target="$target_path" '$2 == target { found=1 } END { exit !found }' /proc/mounts; then
        return 0
    fi
    mount -o bind,nosuid,nodev "$source_path" "$target_path"
}

cleanup_mounts() {
    for target in "$CHROOT_DIR/sys" "$CHROOT_DIR/proc" "$CHROOT_DIR/dev"; do
        if awk -v target="$target" '$2 == target { found=1 } END { exit !found }' /proc/mounts; then
            umount "$target" 2>/dev/null || log -t "$TAG" "Could not unmount $target during desktop shutdown."
        fi
    done
}
trap cleanup_mounts EXIT
trap 'cleanup_mounts; exit 143' TERM
trap 'cleanup_mounts; exit 130' INT


# Force enable Android Freeform Windows at the system level
settings put global enable_freeform_support 1
settings put global force_resizable_activities 1

if [ ! -f "$INIT_MARKER" ]; then
    cmd notification post -S default -t "LucidOS" "setup" "Generating Dynamic Desktop... Stand by."

    mkdir -p "$CHROOT_DIR"
    if [ ! -x "$CHROOT_DIR/bin/bash" ]; then
        if [ ! -s "$TAR_PATH" ]; then
            log -t "$TAG" "Ubuntu base archive missing: $TAR_PATH"
            exit 1
        fi
        if ! tar -xf "$TAR_PATH" -C "$CHROOT_DIR"; then
            log -t "$TAG" "Ubuntu base extraction failed; setup marker was not written."
            exit 1
        fi
    fi

    bind_once /dev "$CHROOT_DIR/dev"
    bind_once /proc "$CHROOT_DIR/proc"
    bind_once /sys "$CHROOT_DIR/sys"
    echo "nameserver 8.8.8.8" > "$CHROOT_DIR/etc/resolv.conf"

    # Ensure internal ADB is running for the bridge
    setprop service.adb.tcp.port 5555
    stop adbd
    start adbd
    sleep 3 # Give ADB a second to wake up

    # 2. The Automated Installer & Shortcut Generator
    chroot "$CHROOT_DIR" /bin/bash << 'EOF'
set -e
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y xfce4 xfce4-goodies plank git dbus-x11 adb wget

# Theme Installations
if [ ! -d /tmp/theme/.git ]; then
    git clone https://github.com/vinceliuice/WhiteSur-gtk-theme.git /tmp/theme
fi
/tmp/theme/install.sh -c Dark -t all -N glassy
if [ ! -d /tmp/icons/.git ]; then
    git clone https://github.com/vinceliuice/WhiteSur-icon-theme.git /tmp/icons
fi
/tmp/icons/install.sh

# Build the Advanced Freeform ADB Bridge
cat << 'BRIDGE' > /usr/local/bin/android-app
#!/bin/bash
adb connect 127.0.0.1:5555 > /dev/null 2>&1
PKG="\$1"

# Ask Android for the exact launch component of the app
ACTIVITY=\$(adb -s 127.0.0.1:5555 shell cmd package resolve-activity --brief \$PKG | tail -n 1)

# Launch the app in Freeform Window Mode (windowingMode 5)
adb -s 127.0.0.1:5555 shell am start --windowingMode 5 -n \$ACTIVITY > /dev/null 2>&1
BRIDGE
chmod +x /usr/local/bin/android-app

# Dynamically generate shortcuts for ALL installed third-party Android apps
adb connect 127.0.0.1:5555 > /dev/null 2>&1
PACKAGES=$(adb -s 127.0.0.1:5555 shell pm list packages -3 | cut -d':' -f2)

mkdir -p /root/Desktop
for PKG in $PACKAGES; do
    # Creates a shortcut using the package name (e.g., com.spotify.music)
    cat << APP > "/root/Desktop/${PKG}.desktop"
[Desktop Entry]
Version=1.0
Type=Application
Name=$PKG
Icon=android
Exec=android-app $PKG
Terminal=false
APP
done
chmod +x /root/Desktop/*.desktop 2>/dev/null || true

# ---------------------------------------------------------
# AUTO-APPLY THE MACOS THEME ON FIRST BOOT
# ---------------------------------------------------------
mkdir -p /root/.config/autostart

cat << 'THEMER' > /root/.config/autostart/mac-theme-setup.desktop
[Desktop Entry]
Type=Application
Name=MacThemeSetup
Exec=bash -c "sleep 2 && xfconf-query -c xsettings -p /Net/ThemeName -s 'WhiteSur-Dark' --create -t string && xfconf-query -c xsettings -p /Net/IconThemeName -s 'WhiteSur' --create -t string && xfconf-query -c xfwm4 -p /general/theme -s 'WhiteSur-Dark' --create -t string && rm /root/.config/autostart/mac-theme-setup.desktop"
Hidden=false
NoDisplay=false
X-GNOME-Autostart-enabled=true
THEMER

chmod +x /root/.config/autostart/*.desktop
# ---------------------------------------------------------

EOF
    SETUP_STATUS=$?
    if [ "$SETUP_STATUS" -ne 0 ]; then
        log -t "$TAG" "Desktop provisioning failed (exit $SETUP_STATUS); setup will retry next activation."
        exit "$SETUP_STATUS"
    fi
    touch "$INIT_MARKER" || {
        log -t "$TAG" "Could not write desktop setup marker."
        exit 1
    }
    cmd notification post -S default -t "LucidOS" "setup" "Desktop initialized. All apps bridged."
else
    # Standard activation: bind each pseudo-filesystem at most once.
    bind_once /dev "$CHROOT_DIR/dev"
    bind_once /proc "$CHROOT_DIR/proc"
    bind_once /sys "$CHROOT_DIR/sys"
    
    setprop service.adb.tcp.port 5555
    stop adbd
    start adbd
fi

# 3. Auto-Launch the ecosystem
am start -n com.termux.x11/com.termux.x11.MainActivity
chroot $CHROOT_DIR /bin/su - root -c "export DISPLAY=:0 && startxfce4 & plank"
