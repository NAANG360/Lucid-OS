# Lucid SystemUI roadmap

## Goal
Turn the Figma Control Center concept (reference node `63:2`) into a native Android 15 notification shade for Redmi Note 7 (`lavender`), using Lucid's lavender/periwinkle canvas, pale cards, electric-blue active states, and dark ink typography. The existing app-level Control Center is only a development bridge; it is not the finished Quick Settings shade.

## Required implementation layers
1. **Quick Settings shade layout** — adapt the QS panel, tile grid, brightness row, header, footer, and notification stack to the device's actual display cutout, density, gesture area, and landscape behavior.
2. **Real tile state** — retain Android's Tile services and system APIs. Never draw pretend Wi-Fi, Bluetooth, mobile-data, battery, or flashlight states.
3. **Brightness and volume** — use SystemUI's existing brightness controller and media/session APIs, including user restrictions and policy handling.
4. **Notifications** — preserve Android notification ranking, grouping, privacy, inline replies, dismissal, heads-up behavior, and accessibility semantics.
5. **Settings entry points** — link to Lucid Settings for customization while retaining all platform-owned configuration paths.
6. **Accessibility** — content descriptions, minimum touch targets, TalkBack order, large text, contrast, reduced animation, and RTL testing.
7. **Lockscreen and shade transitions** — preserve keyguard security, biometric prompts, power menu, screen recording, screenshot and emergency flows.

## Source and build prerequisite
A complete matching Android 15 / LineageOS source checkout and the exact device configuration are needed to safely rebuild SystemUI. The repository's extracted filesystem and APK build workflow are not a substitute for that source tree. Do not patch the live ZIP's `SystemUI.apk` by resource substitution, and do not install the app-level prototype as a replacement for the privileged SystemUI package.

## Acceptance gates
- Build the matching SystemUI target successfully.
- Boot and test on an emulator/device with the exact framework resources.
- Verify quick settings tile state and actions against the platform services.
- Test notification interaction, keyguard, rotation, cutout, dark/light modes, accessibility, and reboot.
- Only then package the new SystemUI into a ROM candidate; boot-test the complete ROM before distributing or flashing.

## Current boundary
The first native Lucid Control Center and Settings dashboard are under `theme/control-center-app/` and `theme/settings-app/`. They are useful functional prototypes and CI artifacts, not a custom system shade. Full SystemUI implementation remains blocked on the matching source/build environment.
