# Lucid Dynamic Island — Redmi Note 7 (lavender)

Notch-aware overlay prototype for the Redmi Note 7's centered waterdrop camera. It reads runtime display metrics rather than assuming a fixed resolution.

## Build/install
The Actions workflow builds `LucidDynamicIsland.apk`. Install it, grant Display over other apps, then tap Enable. This is a prototype, not a replacement for privileged SystemUI. Real media/notification state, lockscreen integration and boot-tested behavior require integration into the matching Android 15 source and device testing.

## Boundaries
- Dynamic island position derives from current display metrics and cutout insets where exposed by Android.
- Tap toggles compact and expanded states; no fake notifications or media state are shown.
- No accessibility service or notification listener is silently enabled.
