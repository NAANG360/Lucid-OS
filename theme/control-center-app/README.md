# Lucid Control Center

Native Android app prototype using the Lucid visual tokens documented in `design/LUCIDOS_VISUAL_SYSTEM.md`.

## Current behavior
- Brightness slider writes `Settings.System.SCREEN_BRIGHTNESS` after the user grants Android's special Modify system settings access.
- Volume slider controls the media stream through `AudioManager`.
- Flashlight button selects a camera that reports flash availability and requests camera permission before toggling the torch.
- Wi-Fi, Bluetooth, Display, Sound, and All settings cards launch the platform's corresponding Settings screens.
- The app registers a Quick Settings tile named **Lucid**. Adding that tile is user-controlled in Android's Quick Settings tile editor; clicking it opens this Control Center.

## Scope boundary
This is a functional app-level control surface, **not a replacement for SystemUI's notification shade**. It cannot silently toggle restricted radios on modern Android. A genuine replacement of the stock QS panel requires a matching SystemUI source patch integrated into the ROM build. No platform APK is replaced by this prototype.

## Permission behavior
The app is not privileged by default. Brightness requires the user to grant Modify system settings; flashlight requires camera permission. Failures are surfaced to the user rather than silently pretending a control succeeded.

## Build
The `Lucid Theme Overlay` workflow builds and signs `LucidControlCenter.apk` as a development artifact using an ephemeral CI key. Do not integrate it into a flashable ROM until compilation, device behavior, permissions, and signing policy have been validated.
