# Lucid Settings

Standalone, pre-integration Settings dashboard for Lucid OS, targeting the Android 15/API 35 base and Redmi Note 7/7S (lavender).

## Design direction
Use the native Settings information architecture as the functional foundation, with Lucid's own dark surfaces, violet accents, concise category cards, search, and a device/build identity header. This takes inspiration from the pattern of custom-ROM feature hubs (Evolution X Evolver and crDroid Settings) without copying their branding or code.

## Current scope
- Searchable categories: connectivity, apps, notifications, display, sound, wallpaper/style, Lucid Control Centre, appearance, security/privacy, location, battery, storage, system and device information.
- Most standard categories deep-link into Android's real Settings screens.
- Lucid Control Centre attempts to launch the standalone Control Centre package.
- Wallpaper integration attempts to launch the platform wallpaper picker.
- Theme page clearly identifies runtime theme switching as pending ROM theme-service integration.

## Limitations
This is a dashboard/companion app, not a replacement for the privileged AOSP Settings package. Several categories open the existing Android Settings screens. A wallpaper picker package or Control Centre package must be present to open those surfaces. The APK build must pass GitHub Actions and be tested on a device before release.