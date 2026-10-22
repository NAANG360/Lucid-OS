# LucidOS Setup app

Native Android first-boot wizard for the Lavender-based LucidOS ROM, rebuilt from the supplied setup-prototype recordings. This pass prioritizes the screen sequence, lavender/cyan/night palette, pill-shaped magenta actions, dark account sheet, moon artwork, and activation animation. Authentication is deliberately deferred.

## Screens and behavior

1. **Welcome** — lavender field, cyan and midnight organic shapes, LucidOS label, and magenta start button.
2. **Wi-Fi** — matching abstract landscape and a native in-app network list with refresh, SSID selection, password entry, and direct connection attempt; it never launches Android Wi-Fi settings. Direct joining requires platform signing and privileged permission allowlisting on modern Android.
3. **Account** — purple moon illustration and dark bottom-panel layout with email, phone, Google, and Apple options. These are visual placeholders; they do not collect or send credentials.
4. **Phone verification placeholder** — dark system-style screen that explicitly states verification is not enabled and does not ask for a number.
5. **Activation** — animated cyan/magenta dots, then a request to mark `lucid_setup_complete` and return to Android Home.

Back navigation is wired between wizard screens. Authentication, real account creation, and SMS verification are intentionally out of scope for this pass. The visuals are a native approximation of the supplied recordings, not a claim of pixel-perfect Figma parity; exact spacing and small text remain limited by the prototype recording scale.

The package is `com.cosmos.setup`, matching the current first-boot launcher and privileged-permission allowlist. Controls use readable 15sp text and 48dp minimum touch targets. The APK is intended for `/system/priv-app/LucidSetup/LucidSetup.apk`.

## Build

Install Android SDK platform 35, build-tools 35.0.0, and a Java compiler. Run:

```sh
ANDROID_HOME=/path/to/android-sdk tools/build_lucid_setup.sh
```

The generated APK is development-signed. It is **not a release artifact**: it must be platform-signed according to the ROM's policy, integrated into a clean-base candidate, and tested to confirm `WRITE_SECURE_SETTINGS` is actually granted.

## Integration boundary

The source and first-boot launcher are under active development. This does not replace the older APK embedded in the current ROM archive, does not modify partition images, and does not establish that the ROM boots. Build success, APK installation, privileged permission grant, first-boot behavior, and device boot validation are separate checks.
