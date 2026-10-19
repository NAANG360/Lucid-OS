# Lucid Settings

Native Android dashboard prototype. The rows use Lucid's lavender surface, rounded white group cards, electric-blue accents, and compact uppercase section labels.

## Current behavior
- Device categories launch Android's actual network, Bluetooth, display, sound, storage, battery, security, location, accessibility, and system pages.
- Personalization links launch Lucid Wallpapers/system-color controls, Lucid Control Center, home-screen settings, notification settings, app management, and device information.
- If a destination is unavailable on the target Android build, the app shows a message instead of crashing.

## Scope boundary
This dashboard does not replace the privileged AOSP/LineageOS Settings package. Settings screens that require system permissions or platform signatures remain owned by Android. Once the exact ROM source tree and signing/build environment are available, the dashboard can be integrated more deeply without risking core Settings functionality.

## Build
The `Lucid Theme Overlay` workflow builds and signs `LucidSettings.apk` as a development artifact using an ephemeral CI key. It is not yet a ROM-integrated or boot-verified system app.
