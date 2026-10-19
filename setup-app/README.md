# LucidOS Setup app

This source implements a native first-boot onboarding flow in the Lucid visual language: pale lavender surfaces, rounded cards, electric-blue actions, Violet Dusk / Blue Alpine artwork, and a low-noise layout adapted for Android rather than copying the iPhone frame literally.

## Screens and behavior

- Welcome screen with the primary Figma-derived Dusk landscape.
- Optional appearance setup for Lucid Blue, Violet Dusk, or Cyan Horizon.
- Optional Dusk or Alpine wallpaper selection; choosing "Keep my current wallpaper" leaves the current wallpaper untouched.
- Finish/skip paths return to Android Home.
- Successful completion is recorded in the secure setting `lucid_setup_complete`.
- The first-boot launcher waits for Package Manager, resolves the real setup activity, and does not mark setup complete just because it started the app.

The app package is `com.cosmos.setup`, matching the current init script and existing privileged-permission allowlist. The APK is intended for `/system/priv-app/LucidSetup/LucidSetup.apk`.

## Build

Install Android SDK platform 35, build-tools 35.0.0, and a Java compiler. Run:

```sh
ANDROID_HOME=/path/to/android-sdk tools/build_lucid_setup.sh
```

The generated APK is development-signed. It is **not a release artifact yet**: install it in the privileged path with the matching `WRITE_SECURE_SETTINGS` allowlist, then verify the permission grant in the target build/device. Follow the ROM's signing policy for release builds; never assume a permission was granted just because the APK compiled.

## Current integration boundary

This commit adds the new source and build script, and hardens the first-boot launcher. It does not replace the APK already embedded in the current ROM archive, does not modify partition images, and does not establish that the ROM boots. The new APK must be built, platform-signed, integrated into the clean-base candidate, and validated as part of the ROM packaging workflow.
