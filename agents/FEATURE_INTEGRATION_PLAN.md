# Lucid OS feature integration plan

**Target:** Xiaomi Redmi Note 7 / Note 7S (`lavender`, Snapdragon 660), Android 15 base direction.

This is an implementation checklist, not a claim that any feature is already integrated or device-tested. The ROM build remains the final integration target; standalone APKs and product-only archives do not satisfy that goal.

## Integration order

### 0. Establish a reproducible, verified build (blocking)
- [ ] Identify and pin the exact Android 15 lavender base ZIP, checksum, device metadata, partition layout, and source revision.
- [ ] Make CI build from the same immutable inputs and retain logs/manifests.
- [ ] Validate every partition image and the final ZIP; compare OTA manifest URL, byte size, and SHA-256 with the public ROM asset.
- [ ] Never publish an artifact as bootable until a physical lavender device boots it and core hardware is checked.

### 1. Lucid UI foundations
- [ ] Compile, sign, inspect, and install-test the Settings and Control Centre APKs.
- [ ] Replace brittle hand-packaging with a repeatable Android build configuration or a tested AOSP module build.
- [ ] Keep Lucid's dark surfaces, restrained lavender/violet accents, typography, spacing, and supplied visual references (including the bubbles reference); avoid generic ad-like UI.

### 2. Settings and system theming
- [ ] Turn Settings into a real customization hub: searchable categories, appearance profiles, accent palette, icon/shape options, lock-screen and QS controls.
- [ ] Implement the theme service/overlay integration and persist user selections across reboot.
- [ ] Keep standard Android Settings routes working when an optional Lucid component is not installed.
- [ ] Add wallpaper browsing, preview, apply, and palette integration.

### 3. Control Centre, notifications, and lock screen
- [ ] Implement the Lucid visual treatment in the actual SystemUI Quick Settings/notification shade; the companion APK is not a replacement.
- [ ] Wire tiles to real Android services, implement brightness and volume with permission-aware handling, and verify status/navigation bar insets.
- [ ] Add lock-screen clock/date, notification treatment, wallpaper, and customization controls.
- [ ] Implement Super Device-inspired radial discovery and drag-to-connect only alongside real, authenticated transport protocols; discovery alone is not a connection.

### 4. Launcher and system apps
- [ ] Inventory all shipped system apps before changing them; build a per-app integration matrix.
- [ ] Preserve standard launcher behavior: workspace placement, widgets, folders, app drawer, long-press actions, gestures, accessibility, and rotation.
- [ ] Apply the Lucid design system consistently to every app in scope, not only Settings and Control Centre.
- [ ] Provide a dedicated wallpaper picker and correct About/ROM/device/build details.

### 5. Updater and releases
- [ ] Keep the OTA manifest pointing to the ROM ZIP, not the recovery updater installer.
- [ ] Verify public ROM URL, SHA-256, size, version, device assertion, download resume/error handling, and signature policy.
- [ ] Maintain the recovery installer/updater-binary and the in-system updater as separate, version-compatible components.
- [ ] Do not claim seamless/system-running installation unless the target's A/B or recovery mechanism genuinely supports it; implement safe recovery handoff when it does not.

### 6. Lucid Desktop
- [ ] Audit the existing rootfs and scripts before integrating them into the ROM.
- [ ] Gate privileged operations behind an explicit Developer Options toggle and ensure disabling it revokes desktop-side root access.
- [ ] Make first boot idempotent; fix DNS/network access using supported Android networking rather than hard-coded hosts entries.
- [ ] Validate chroot startup, display/input, package installation, app launch, stop/restart, and cleanup.

### 7. Release gates
- [ ] CI build and static/package checks pass.
- [ ] Final ZIP passes integrity and partition/metadata validation.
- [ ] OTA manifest exactly matches the public ROM asset.
- [ ] Recovery installation succeeds on compatible recovery.
- [ ] Physical-device boot and hardware smoke tests pass.
- [ ] Release notes list known limitations and hashes.

## Current verified boundary
Settings and Control Centre are prototypes in a draft PR. Their successful CI builds, SystemUI integration, full theme service, all-app refresh, complete ROM integration, recovery flashing, and physical-device boot must each be verified independently.