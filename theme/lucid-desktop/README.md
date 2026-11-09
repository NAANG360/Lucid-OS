# Lucid Desktop Controller

Native Lucid-styled Android controller for starting and diagnosing the Ubuntu chroot.

- Rootfs: `/data/local/lucid_desktop`
- Archive: `/system/usr/lucid/ubuntu-base-arm64.tar.gz`
- Controller: `/system/bin/lucid_desktop.sh`
- Display target: `:0`, compatible with Termux:X11
- Log: `/data/local/tmp/lucid-desktop.log`

The app calls a fixed ROM controller through the device's configured root provider. It does not create root or bypass Android permissions. The ROM must install the script with executable permissions and configure its chosen root provider and user-consent policy.

Android does not ship an X11 server. This app implements Lucid's desktop startup/control flow, while Termux:X11 is the current supported display server. A bundled X server requires a separate native server implementation and device-specific display/input integration.

Startup does not run `apt-get`, avoiding the earlier chroot socket and DNS failures. It seeds an empty `resolv.conf`, verifies the extracted Bash binary and XFCE launcher, mounts `/dev`, `/proc`, and `/sys`, prefers `dbus-run-session`, and records diagnostics. These changes cannot fix an Android kernel or SELinux denial without corresponding device-ROM policy and must be tested on lavender.

This repository currently does not include the full Android device source/build tree. The app and script are integration components, not a claim of a bootable ROM.