# LucidOS root integration

## Root implementation boundary

Changing `/system/xbin/su` to mode 06755 does not create a privileged daemon, grant manager, or the SELinux policy required for Android root. Remounting a dynamic/read-only `/system` partition during a property change is not a reliable root mechanism.

The previous `lucid_root_toggle.sh` attempted to remount `/system` and toggle permissions. The replacement is fail-closed: it detects a supported Magisk installation, sets `sys.lucid.root_available`, and never alters system-file permissions. It does not claim to install or enable root when Magisk is absent.

## Requirements for a rooted release

1. Patch the exact, verified lavender boot image with a compatible official Magisk release.
2. Preserve the original boot image and record its SHA-256 before patching.
3. Validate the patched image structure and device target before packaging.
4. Include the matching Magisk manager and preserve its normal per-app grant UI.
5. Boot-test the exact image on lavender before publishing it as a rooted ROM.

The repository audit still identifies an incomplete product payload and has not proven that the ROM boots on-device. This change removes the unsafe fake root toggle; it does **not** claim that the current ROM is rooted or flash-ready. A verified boot image and device-specific boot test are prerequisites to shipping a rooted build.