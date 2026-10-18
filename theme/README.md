# LucidOS theme build

This directory contains the first buildable theme layer for LucidOS. The source design reference is the LucidOS Control Center family in the linked CosmosOS Figma file; only Lucid-branded artwork and tokens are used.

## Palette profiles

The palette generator uses Material Color Utilities and emits the 65 framework accent/neutral resources consumed by the framework RRO.

From this directory:

- `npm run generate-palette` — default Lucid Blue
- `npm run theme:blue` — `#0866FF`
- `npm run theme:violet` — Violet Dusk, `#7657FF`
- `npm run theme:cyan` — Cyan Horizon, `#00AFC8`

These are **build-time profiles**, not a runtime theme switcher. To change the generated APK, select a profile and rebuild the overlay. The profile list is in `profiles.json`.

## Build outputs

The GitHub Actions workflow produces:

- `LucidOS-Framework-Theme-Overlay.apk` — framework palette RRO, signed with an ephemeral CI development key
- `LucidWallpapers.apk` — native wallpaper picker with Dusk and Alpine choices
- `LucidOS-bootanimation.zip` — generated boot animation
- a report and emulator screenshot/logs when the smoke test passes

**These are development artifacts, not yet a flashable ROM update.** The ephemeral signing key is not the ROM's platform key. Do not install a framework overlay into the ROM unless the target overlay policy and signature/installation path have been validated.

## Device-shaped emulator

CI boots a generic Android API 35 x86_64 emulator with a 1080×2340 display and 440 dpi to approximate the Redmi Note 7/7S display class. It tests APK installation, Activity launch, and crash-free rendering. It is **not** a Redmi Note 7S hardware emulator: it does not model the Snapdragon 660, Xiaomi boot chain, lavender device tree, proprietary vendor services, or the ROM's logical partitions. Only a real lavender device or a verified device-specific test setup can prove that the ROM itself boots.

## Current ROM blocker

The cloud partition audit found the product filesystem is incomplete/corrupt after reconstructing the current ZIP: `e2fsck -fn` reports invalid extent nodes and a corrupted root directory, and the mounted product tree is empty. The installer wipes/recreates the dynamic partition metadata, so the missing product blocks cannot be assumed to survive from the prior ROM. Repair the product payload/OTA packaging before attempting another flash. See `docs/ROM_INSTALLER_FINDINGS.md`.
