# Clean Android 15 base → LucidOS migration

## Selected base family

Use a **vanilla Android 15 ROM for Redmi Note 7/7S (lavender)** from the Redmi Note 7/7S Updates channel, not the current LucidOS ZIP as the base. The channel has two relevant Android 15 candidates:

- **LineageOS 22.2 Unofficial, vanilla/encrypted, dynamic, 4.19 kernel**, posted 24 December 2025: https://t.me/s/redminote7Updates?q=%23LineageOS
- **LineageOS-Ext community Android 15, dynamic, 4.19 kernel** (the post does not explicitly label the package vanilla), posted 30 March 2025: https://t.me/s/RedmiNote7Updates/7503

The repository's archived lineage-22.2-20251222-UNOFFICIAL-lavender.zip is the December 2025 LineageOS 22.2 package. For the requested different base, investigate the LineageOS-Ext Android 15 vanilla/dynamic build first; do not silently substitute the already-archived LineageOS 22.2 ZIP. Resolve its actual Download link and verify the exact file before using it. The channel post is evidence of the build label, not proof that the download is still available or safe.

## Merge contract

The clean ROM is the foundation, not the finished product. Keep the existing LucidOS source archive immutable and assemble a separate candidate. Preserve and deliberately reapply, at minimum:

- init.lucid.rd if present in the original release/boot ramdisk, plus init.lucid.rc and every Lucid init trigger/service.
- lucid_desktop.sh, lucid_firstboot.sh, lucid_root_toggle.sh, their executable modes and required SELinux labels.
- The Lucid setup APK and matching privileged-permission allowlist. The rebuilt wizard APK is not yet integrated merely because its source exists.
- Ubuntu base archive and all desktop/chroot dependencies.
- Lucid wallpapers, wallpaper APK, boot-animation source and generated ZIP, theme/RRO assets, configuration and permissions.
- Any Lucid property/build changes, overlay resources, scripts, binaries, symlinks and permission/context changes found in the actual Lucid ZIP and extracted boot ramdisk, even if not currently represented in the tracked source tree.

## Candidate acceptance gates

1. Verify the downloaded ZIP's SHA-256 and ZIP integrity; record the exact source URL, Telegram post, filename, size and digest.
2. Check metadata and extracted build properties for Android 15 / SDK 35 and lavender. Check installer assumptions for retrofit dynamic partitions against the target recovery/device.
3. Inventory the base and Lucid release independently. Extract and scan boot ramdisks as well as all partition payloads; a Git-tree search alone cannot find release-only binaries.
4. Reapply Lucid changes as explicit overlay/patch operations. Do not copy the entire old system-tree over the base: that risks replacing Android 15 framework/system components with stale files.
5. Build complete target partition images from complete filesystems. Never label a partial block-OTA payload as a full image. In particular, the old Lucid product payload cannot be used as a blank-image source.
6. Compare the candidate against the clean base and emit a machine-readable preserve/add/replace/remove report. Any unexplained Lucid-only removal blocks packaging.
7. Validate filesystem structures, transfer-list coverage, installer partition sizes and all archive entries before producing a candidate ZIP. Keep the result unsigned/development-only until signing and device tests are completed.

## Current evidence that affects the merge

- Existing LucidOS archive: LucidOS-unsigned.zip, SHA-256 6e9fa4bc2c7dffee9671a02f45bf3b4b7f881b33546cde33b2ea2c9f8ca953b1.
- Archived LineageOS 22.2 baseline: lineage-22.2-20251222-UNOFFICIAL-lavender.zip, SHA-256 b8029d7dd1d5496c31910bb05861ff7114d166e8397827ca51a163bacea6e6b7.
- Existing Lucid product payload writes only 123,902 of 601,750 filesystem blocks and fails ext4 validation when reconstructed from an empty image. The base migration must build product from a complete base filesystem/tree or a full image.
- The old installer resets retrofit dynamic-partition groups and flashes boot/dtbo/vbmeta. Reusing it without reconciling the selected base's partition metadata is not acceptable.
- The exact string init.lucid.rd was not found in the tracked tree. It must still be searched in the original Lucid release and boot ramdisk and preserved if found.


## Explicit overlay staging tool

`tools/stage_lucid_overlays.py` is the first controlled merge primitive. It stages only an allowlist of Lucid-owned init hooks, scripts, permissions, the existing setup APK, and the Ubuntu base directory. It deliberately does not copy the old system tree wholesale or replace Android framework, Settings, SystemUI, vendor, boot, DTBO, or vbmeta. It defaults to a dry-run inventory and records each file's SHA-256, size, mode, and whether it would add/replace/leave unchanged. Once a complete base filesystem has been extracted to a directory containing `system/`, run:

```bash
python3 tools/stage_lucid_overlays.py --base-root /path/to/extracted-base --report lucid-overlay-plan.json
```

Inspect the report, then use `--apply` to copy the allowlisted files. This only stages source files; image creation still needs correct Android ownership, SELinux labels/file contexts, product overlay integration, and filesystem validation. It does not itself create a flashable ROM.

### Direct-download fallback identified

The channel's 9 November 2024 LineageOS 22.0 / Android 15 post links to this direct SourceForge download: https://sourceforge.net/projects/test.lavender-roms.p/files/Rom/Lineage-22/enforcing-test/lineage-22.0-lavender-20241109-2232.zip/download. It is explicitly described as a test build, so it is retained as a fallback for comparison, not the preferred production base. The preferred different base remains the March 2025 LineageOS-Ext dynamic Android 15 build if its actual download can be confirmed as vanilla; resolve and verify that build's actual download rather than downgrading the foundation merely because this older link is direct.
