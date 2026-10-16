#!/system/bin/sh

CHROOT_DIR="/data/local/lucid_desktop"
TAR_PATH="/system/usr/lucid/ubuntu-base-arm64.tar.gz"

# Force enable Android Freeform Windows at the system level
settings put global enable_freeform_support 1
settings put global force_resizable_activities 1

if [ ! -d "$CHROOT_DIR/bin" ]; then
    cmd notification post -S default -t "LucidOS" "setup" "Generating Dynamic Desktop... Stand by."
    
    mkdir -p $CHROOT_DIR
    tar -xf $TAR_PATH -C $CHROOT_DIR
    
    mount -o bind,nosuid,nodev /dev $CHROOT_DIR/dev
    mount -o bind,nosuid,nodev /proc $CHROOT_DIR/proc
    mount -o bind,nosuid,nodev /sys $CHROOT_DIR/sys
    echo "nameserver 8.8.8.8" > $CHROOT_DIR/etc/resolv.conf

    # Ensure internal ADB is running for the bridge
    setprop service.adb.tcp.port 5555
    stop adbd
    start adbd
    sleep 3 # Give ADB a second to wake up

    # 2. The Automated Installer & Shortcut Generator
    chroot $CHROOT_DIR /bin/bash << 'EOF'
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y xfce4 xfce4-goodies plank git dbus-x11 adb wget

# Theme Installations
git clone https://github.com/vinceliuice/WhiteSur-gtk-theme.git /tmp/theme
/tmp/theme/install.sh -c Dark -t all -N glassy
git clone https://github.com/vinceliuice/WhiteSur-icon-theme.git /tmp/icons
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
chmod +x /root/Desktop/*.desktop

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

    cmd notification post -S default -t "LucidOS" "setup" "Desktop initialized. All apps bridged."
else
    # Standard boot mounts
    mount -o bind,nosuid,nodev /dev $CHROOT_DIR/dev
    mount -o bind,nosuid,nodev /proc $CHROOT_DIR/proc
    mount -o bind,nosuid,nodev /sys $CHROOT_DIR/sys
    
    setprop service.adb.tcp.port 5555
    stop adbd
    start adbd
fi

# 3. Auto-Launch the ecosystem
am start -n com.termux.x11/com.termux.x11.MainActivity
chroot $CHROOT_DIR /bin/su - root -c "export DISPLAY=:0 && startxfce4 & plank"
