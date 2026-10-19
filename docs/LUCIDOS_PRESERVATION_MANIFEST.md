# LucidOS preservation manifest and clean-base merge gate

**Status:** inventory is partial until the complete LucidOS ZIP, boot ramdisk, reconstructed partition filesystems, original LineageOS ZIP, and all theme workflow artifacts have been scanned together. This file is a preservation contract, not a claim that every hidden artifact has already been recovered.

## Rule: clean base must not replace the Lucid payload

A vanilla LineageOS archive is a comparison/base candidate only. Do not overwrite the current LucidOS archive or replace the project tree with it. Build in a separate candidate directory/branch, keep the original archives immutable, and merge the Lucid changes onto the clean base. Every unexplained deletion of a Lucid-only path is a hard failure.

## Confirmed tracked LucidOS implementation

The current repository tree was queried recursively at commit `b38492e67f0732996dcf4bfef6a3014c9c1c39a8` (3,429 entries; GitHub reported the tree was not truncated).

### Init / system hooks and scripts
- `system-tree/system/etc/init/init.lucid.rc` — 685 bytes, blob `1d594048a4567082fb80b648940a11ef49c84272`.
- `system-tree/system/bin/lucid_desktop.sh` — 3,677 bytes, blob `ca5bc2ede9c7fe16ae6956196f245076aba50d01`.
- `system-tree/system/bin/lucid_firstboot.sh` — 385 bytes, blob `039282a9ea31cd0a17716170904ecc3770ff46c9`.
- `system-tree/system/bin/lucid_root_toggle.sh` — 154 bytes, blob `6487a1a90288b3e5f30df06513a570e7bd436bcd`.
- `system-tree/system/etc/permissions/lucid-privapp-permissions.xml` — 428 bytes, blob `bffd782c4e7f9ca7b52bb0b580daa138049013e9`.
- `system-tree/system/priv-app/LucidSetup/LucidSetup.apk` — 6,757,252 bytes, blob `5587f5d67ac89eead2ef10ef1fb1e734dafc9484`.
- `system-tree/system/usr/lucid/ubuntu-base-arm64.tar.gz` — 27,660,560 bytes, blob `3ccc455c772485a4e1000d4aee444168e0ddacc1`.
- `system-tree/init`, `system-tree/init.environ.rc`, and `system-tree/init.recovery.qcom.rc` are also present and must be compared before any init/ramdisk changes.

**Name mismatch to resolve:** the exact string `init.lucid.rd` was not found in the recursive tracked Git tree. The tracked file found is `system-tree/system/etc/init/init.lucid.rc`. This does not establish that `init.lucid.rd` is absent from the release ZIP, boot ramdisk, or any uploaded/local source. Search those separately and preserve the exact file if found; do not silently rename or discard it.

### First-boot setup and Figma-derived personalization
- `setup-app/AndroidManifest.xml`, `setup-app/res/values/styles.xml`, and `setup-app/src/com/cosmos/setup/MainActivity.java` now define a new Figma-inspired onboarding app under package `com.cosmos.setup`.
- `tools/build_lucid_setup.sh` and `.github/workflows/lucid-setup-app.yml` build and validate a development-signed APK artifact. This artifact is not yet integrated into the ROM image and must not be mistaken for a replacement already present in `system-tree/system/priv-app/LucidSetup/LucidSetup.apk`.
- The current app source now follows the supplied setup recordings: lavender/cyan/midnight welcome and Wi-Fi screens, a purple moon with dark account sheet, a non-collecting phone-verification placeholder, and an animated activation screen. Wi-Fi opens Android's actual Wi-Fi panel/settings; account and phone authentication are explicitly deferred. Completion uses the secure setting `lucid_setup_complete`. The first-boot launcher waits for Package Manager and resolves the activity rather than assuming it is immediately available.
- Integration is a coupled change: replace the existing APK with the new build in the clean-base candidate, preserve the existing `com.cosmos.setup` privileged-permission allowlist, verify `WRITE_SECURE_SETTINGS` is actually granted, and only then rely on the completion setting. Do not ship the new launcher with the old APK and assume the new completion behavior exists.

### Boot animation and Figma-derived artwork
- `tools/generate_bootanimation.py` — 4,072 bytes, blob `f4b859ac63d4373072c94e5db5d60d30386b61ff`.
- `theme/assets/lucid-wallpaper-dusk.jpg` — primary Figma-derived wallpaper, blob `9857c6e8a3d02c683f2730cefb1d2f56c7b16224`.
- `theme/assets/lucid-wallpaper-alpine.jpg` — alternate wallpaper, blob `ef7939c16d491c514a2d0328e81b5e4885674667`.
- `theme/assets/README.md` records the source Figma nodes: wallpaper 4 node `553:124` (image fill `553:138`) and wallpaper 1 node `551:34` (image fill `553:128`).
- `.github/workflows/lucid-theme-overlay.yml` generates `LucidOS-bootanimation.zip` and publishes it as a short-retention workflow artifact named `lucid-theme-overlay-prototype`; it is generated from the dusk wallpaper, not currently tracked as a binary file in Git.
- `theme/wallpaper-app/` contains the native Lucid Wallpapers app source; `theme/privapp-permissions-lucid.xml` and `theme/profiles.json` are related integration/configuration inputs.
- `theme/flashable/` contains a separate prototype flashable package and must be inventoried and reviewed before reuse.

Preserve both the generator/source artwork and any generated animation binary. Before choosing the stock animation instead, extract and hash the original animation from the baseline ROM and compare it with the generated Lucid animation. Default is to retain the generated Lucid animation if it validates and fits the target; stock animation is only the fallback if the Lucid artifact is missing or fails validation. Do not replace either silently.

### Other tracked custom implementation
- `system-tree/system/bin/bootanimation` and `system-tree/system/lib64/libbootanimation.so` are platform binaries, not the Lucid animation asset; compare them to the baseline rather than treating them as custom replacements automatically.
- `design/LUCIDOS_VISUAL_SYSTEM.md`, `theme/README.md`, `theme/rro/`, `theme/scripts/`, `theme/wallpaper-app/`, `theme/config.json`, `theme/package.json`, and the theme build workflows are project inputs and must remain available.
- The repository contains a Figma implementation map pointing at the file `https://www.figma.com/design/UPUZotxcIfcIdElWklOHFN/cosmosOS`. Only Lucid-branded assets are intended for the ROM; unrelated cosmosOS/DynaPunch branding and phone mockup frames are explicitly excluded.

## Required exhaustive scan before creating a new ROM candidate

1. Inventory every entry and hash in the original LineageOS ZIP and current LucidOS ZIP, including installer scripts, metadata, boot/dtbo/vbmeta, all transfer lists/payloads, and any full images.
2. Extract the current LucidOS boot image ramdisk and search all files and contents for `init.lucid.rd`, `init.lucid.rc`, `lucid`, custom init services, fstab entries, properties, shell scripts, binaries, APKs, permissions XML, SELinux policy/context changes, and animation references.
3. Reconstruct and validate every relevant Lucid partition, then enumerate all files in system, system_ext, product, vendor, odm, and ramdisk. Compare against the baseline by path, content hash, mode, owner/group, symlink target, capabilities, and SELinux context where available.
4. Inventory all project assets and generated artifacts: Figma-derived source images, boot animation ZIPs, wallpaper APKs, overlay APKs, flashable ZIPs, scripts, manifests, configs, and workflow artifacts. Do not rely on Git tree search alone for binaries that only exist in releases or short-lived Actions artifacts.
5. Produce a machine-readable preserve/replace/add/remove report. Every Lucid-only path must be classified and retained by default. Any proposed removal requires evidence it is obsolete or incompatible and must be called out explicitly; never silently drop it.
6. Keep the baseline ROM and original LucidOS archive unchanged. Apply changes to a separate candidate and block packaging if any required preservation item is missing, if the file manifest has unclassified differences, or if any reconstructed filesystem fails validation.
7. Verify the resulting ZIP and all partition images before any device test. Do not flash a candidate merely because ZIP integrity or ext4 checks pass.

## Current known ROM issue

The existing LucidOS product payload writes only 123,902 of 601,750 declared filesystem blocks and reconstructs to a corrupt ext4 filesystem from an empty image. This is a serious packaging defect, but not proof it is the only boot blocker. The new-base comparison must solve payload completeness and installer/partition consistency without discarding the custom files listed above.

### Core runtime hardening
- `system-tree/system/etc/init/init.lucid.rc` now resets `service.adb.tcp.port` to `-1` and restarts `adbd` when `sys.lucid.casting` becomes `0`, before clearing the persistent Lucid root toggle. This limits the desktop bridge's ADB-over-TCP exposure to the active casting session rather than leaving port 5555 enabled after casting stops.
- This is a source-level init change only. It has not yet been boot-tested on lavender; verify USB ADB recovery, cast start/stop behavior, and init action ordering in a device or emulator log before including it in a release.
