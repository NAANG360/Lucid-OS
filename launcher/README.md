# LUCID Launcher — DynaPunch direction

An independent Android launcher module for LucidOS. Visual direction selected from the DynaPunch family in the LucidOS Figma canvas: deep ink surfaces, restrained plum/lilac highlights, spacious editorial type, soft rounded widget panels, and a compact search dock.

## Current implementation
- Declares itself as a selectable Android HOME app.
- Queries launchable apps and starts them.
- Includes home screen, favourite-app row, app drawer, and live app-name filtering.
- Uses responsive Compose layout for phone-sized displays.
- Targets Android 8.0+ (API 26), compile/target SDK 35.

## Build
Open this `launcher` directory as a Gradle project in Android Studio, or run:
`gradle :app:assembleDebug`

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Honest scope notes
This is an early functional prototype, not a finished OEM launcher. Weather and calendar cards are visual placeholders; live widget hosting, drag-and-drop placement, folder editing, gesture customization, adaptive icon masks, wallpaper selection, and long-press actions remain future work. The Figma MCP plan hit its call limit during inspection, so the first pass uses the DynaPunch structure described in the retrieved canvas metadata rather than claiming pixel-perfect asset extraction.
