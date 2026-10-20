# LucidOS chroot privilege boundary — implementation status

## Security invariant

The intended design is: Linux processes may be UID 0 *inside the Linux root filesystem*, while ordinary Android apps never receive a host `su` capability. Turning the feature off must stop the Linux session and prevent it from being started again until explicitly enabled.

A chroot alone is **not** a security boundary. A process with Android-host UID 0 and broad SELinux permissions can still affect Android outside the chroot. A property check in a shell script is a useful fail-closed guard, but it is not a root manager or a privilege sandbox.

## Current branch status

- `persist.lucid.chroot_root` is checked by the desktop launcher and init stops the service when the property becomes `0`.
- `system-tree/system/bin/lucid_root_toggle.sh` deliberately fails closed. The legacy `su` binary and remount/chmod approach are not a supported toggle implementation.
- The repo snapshot does not include AOSP `packages/apps/Settings` / `frameworks/base` source or a complete root-manager backend. Therefore this branch **does not yet add a real Developer Options switch**. Setting the property manually is not equivalent to that UI.
- The init service currently runs as host root in the `su` SELinux domain. That is broader privilege than the intended invariant. Do not describe this as root-isolated or enable it in a release until a dedicated, audited SELinux domain and narrow privileged helper are integrated and tested.
- The desktop launcher currently uses a TCP ADB bridge on port 5555 for Android app discovery. Firewall rules are defense-in-depth, not proof that the bridge is loopback-only. This must be removed or replaced with a Unix-domain/local-only bridge before calling the design hardened.
- A chroot process can also access any host paths deliberately bind-mounted into it. Keep host mounts minimal and never bind sensitive Android paths into the Linux root filesystem.

## Required completion work

1. Add a real Settings/Developer Options preference in the actual Android source tree. It should write a dedicated property through a narrowly scoped, permission-checked system service—not shell out to unrestricted `su`.
2. Replace the host-root shell service with a small privileged helper in a dedicated SELinux domain. It should validate fixed paths and arguments, create only required mounts, enter the root filesystem, drop capabilities where possible, and reject arbitrary commands/paths.
3. Define the property in `property_contexts`, authorize only the intended Settings/system component to change it, and default to off after clean install and reset.
4. On disable, terminate the session and all descendants, unmount only known mounts, remove local bridge state, and verify the setting survives reboot as off.
5. Add static tests plus device tests for off/on/off, reboot, SELinux enforcing, ordinary-app denial, and no reachable TCP listener. Until then, the feature is experimental and must not be represented as secure root isolation.

## About Device page

The current About screen is a standalone Java app built by `.github/workflows/lucid-about-page.yml`. It is not yet Android Settings' built-in About phone screen and the repo snapshot lacks the Settings source needed to replace that page. The APK workflow artifact is useful for previewing the design, but it is not automatically included in the ROM image. ROM integration requires a product/build definition that packages the APK (or the actual Settings source patch).
