# Clean Android 15 base → LucidOS migration

## Base candidates (none accepted yet)

Use a **verified vanilla Android 15 ROM for Redmi Note 7/7S (lavender)** from the Redmi Note 7/7S Updates Telegram channel, not the current LucidOS ZIP as the base. No candidate is accepted yet: channel/build labels are discovery evidence, and the actual archive must pass the acceptance gates below.

- **First candidate — Rising-Revived 6.3 vanilla/dynamic**, posted 19 March 2025 in the device update channel. The post labels it Android 15, Vanilla, Dynamic, 4.19. Direct archive: https://sourceforge.net/projects/termux-lavender/files/Rising-Revived/15.0/RisingOS_Revived-6.3-250319-0738-VANILLA-COMMUNITY-lavender.zip/download. Inspect metadata, updater assumptions, boot/kernel compatibility and actual package contents before accepting it.
- **Second candidate — LineageOS 22.2 lavender**, dated 12 June 2025, from the pix106 SourceForge project: https://sourceforge.net/projects/lavender-roms/files/Rom/4.19/Lineage-22.2/lineage-22.2-lavender-20250612-1450.zip/download. This is a distinct build from the archived December 2025 package; verify Android 15, vanilla/no-GApps status, dynamic layout and device assertions.
- **Fallback — LineageOS-Ext Android 15 dynamic**, posted 30 March 2025 in the same Telegram channel. Resolve its current download asset and inspect it before use.
- The channel also lists **LineageOS 22.2 Unofficial, vanilla/encrypted, dynamic, 4.19 kernel**, posted 24 December 2025. That is the same dated build family as the repository's archived `lineage-22.2-20251222-UNOFFICIAL-lavender.zip`, so it is not the requested different base.

The first audit workflow is configured to download the Rising-Revived 6.3 candidate. It records the effective URL, SHA-256, ZIP structure, metadata and installer signals. It does not auto-accept a candidate or produce a flashable ROM. The channel post is evidence of a build label, not proof that the binary is safe or actually matches every label.

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

`tools/stage_lucid_overlays.py` is the first controlled merge primitive. It stages only an allowlist of Lucid-owned init hooks, scripts, permissions, the existing setup APK, and the Ubuntu base directory. It deliberately does not copy the old system tree wholesale or replace Android framework, Settings, SystemUI, vendor, boot, DTBO, or vbmeta. It defaults to a dry-run inventory and records each file's SHA-256, size, mode, and whether it would add/replace/leave unchanged. It also accepts repeatable `--extra-overlay SOURCE=DEST` mappings for locally available generated artifacts (for example, the output of the theme workflow). Once a complete base filesystem has been extracted to a directory containing `system/`, run:

```bash
python3 tools/stage_lucid_overlays.py --base-root /path/to/extracted-base --report lucid-overlay-plan.json
```

Inspect the report, then use `--apply` to copy the allowlisted files. Generated theme outputs can be added explicitly after they have been downloaded from the theme workflow artifact, for example:

```bash
python3 tools/stage_lucid_overlays.py --base-root /path/to/extracted-base \
  --extra-overlay /path/to/LucidWallpapers.apk=system/app/LucidWallpapers/LucidWallpapers.apk \
  --extra-overlay /path/to/LucidOS-bootanimation.zip=system/media/bootanimation.zip \
  --report lucid-overlay-plan.json
```

The wallpaper app and boot animation are suitable candidates for staging; the static framework RRO remains excluded until target overlay policy/idmap and signature behavior are validated. This only stages files; image creation still needs correct Android ownership, SELinux labels/file contexts, privileged permission integration, product overlay integration, and filesystem validation. It does not itself create a flashable ROM.

### Direct-download fallback identified

The channel's 9 November 2024 LineageOS 22.0 / Android 15 post links to this direct SourceForge download: https://sourceforge.net/projects/test.lavender-roms.p/files/Rom/Lineage-22/enforcing-test/lineage-22.0-lavender-20241109-2232.zip/download. It is explicitly described as a test build, so it is retained as a fallback for comparison, not the preferred production base. The Rising-Revived 6.3 vanilla/dynamic package is the first alternative to audit; the June 2025 pix106 LineageOS 22.2 package remains a secondary candidate. The March 2025 LineageOS-Ext dynamic Android 15 build remains the fallback if the June package does not meet the required vanilla/dynamic criteria.
