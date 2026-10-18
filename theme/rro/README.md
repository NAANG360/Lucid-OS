# LucidOS framework theme overlay

This is the first system-resource overlay prototype for the Android 15 / SDK 35 system declared by the current extracted tree.

## Figma direction used

- **Quick Settings reference:** `Control Center` (Figma node `63:2`) — lavender/periwinkle surfaces, white translucent cards, electric-blue active tiles and sliders.
- **Default wallpaper candidate:** `wallpaper 4` (Figma node `553:124`), because the Homescreen frame (`330:111`) uses the same dusk valley/moon image and its purple/cyan landscape coordinates with the translucent widgets.
- **Alternate wallpaper:** `wallpaper 1` (Figma node `551:34`), the dramatic alpine image.
- **Settings:** `iPhone 14 - Settings` (`152:2`) is only a sparse visual reference; Android Settings navigation and real preference rows must remain native.

The Figma file also contains cosmosOS branding and demo data. Those logos, fake notifications, mock device frames and sample status values are not included as LucidOS system assets.

## What this prototype changes

The overlay overrides Android's dynamic system accent and neutral palettes so Settings/SystemUI components that consume the framework palette can adopt Lucid blue, lavender and cyan.

It does **not** replace Settings/SystemUI code, create new Quick Settings tile behavior, or guarantee a particular panel layout. Those require the actual target app resources/source and device validation.

## Build

The `Lucid Theme Overlay` GitHub Actions workflow compiles the RRO against the committed `framework-res.apk`, signs the APK for artifact inspection, and uploads a build report. A successful compile is not proof that the overlay's targetName/resource policy is accepted at runtime.

Do not flash the artifact until the runtime overlay policy and rollback path have been verified on the actual device.
