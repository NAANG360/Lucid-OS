# Installer and partition audit

This is based on the unsigned release ZIP's actual file list and updater script, inspected in a GitHub-hosted runner. It is preliminary until the boot image and AVB metadata report is reviewed.

## Confirmed from the ZIP

- The archive has 27 entries and passes `unzip -t` with no compressed-data errors.
- It is a block-based incremental OTA: it contains `system.new.dat.br`, `system.transfer.list`, `system.patch.dat` and matching vendor/product/odm/system_ext files. It does **not** contain standalone full system/vendor/product images.
- The installer targets `lavender` and requires `ro.boot.super_partition=system`, i.e. a recovery/device configuration that supports retrofit dynamic partitions.
- It writes `super_dummy.img` to the physical `system` partition, applies a dynamic-partition operation list, and updates logical `system`, `vendor`, `product`, `odm`, and `system_ext`.
- It flashes `boot.img`, `dtbo.img`, and `vbmeta.img`.
- There is **no cust image, cust transfer list, cust logical partition, or cust path referenced in the archive/updater script**.

## Dynamic partition layout requested by the installer

| Partition | Requested size |
| --- | ---: |
| system | 1,146,437,632 bytes |
| vendor | 797,704,192 bytes |
| product | 2,504,368,128 bytes |
| odm | 1,523,712 bytes |
| system_ext | 624,320,512 bytes |
| Group maximum | 6,773,800,960 bytes |

The requested logical partition sizes total 5,074,354,176 bytes, leaving 1,699,446,784 bytes below the group maximum. These entries describe the installer's intended layout; they do not by themselves prove that the device's current super metadata matches it.

## Boot-image structural check

The archive's `boot.img` is 64 MiB and begins with the expected `ANDROID!` magic. Parsing its legacy v0 header gives a 4,096-byte page size, a 16,537,691-byte kernel, and a 1,815,853-byte ramdisk. Its OS-version word decodes to Android 15.0.0 with a December 2025 patch-level encoding. The `vbmeta.img` begins with `AVB0` and has a structurally plausible 8 KiB container.

This **does not prove the kernel/ramdisk is compatible with lavender or that AVB will accept the images**, but it means the header is not obviously malformed. The header data is consistent with the declared SDK 35 system more than with the Android 10 fingerprint string. Do not treat the header as the root cause without checking the kernel load addresses, ramdisk contents, AVB flags/descriptors, and recovery logs.

## Boot image and AVB findings

- The boot image has a valid `ANDROID!` legacy header, a 4,096-byte page size, a 16.5 MB gzip-compressed kernel, and a 1.8 MB gzip-compressed ramdisk. Its OS-version word encodes Android 15.0.0 / December 2025.
- The kernel identifies itself as `4.19.325-st-San-Kernel-Kurotsuki-R1.1.107`. The upstream release page identifies that kernel build as intended for the Redmi Note 7 (`lavender`), so the version number alone is not evidence of a wrong-device kernel. See the [San-Kernel Kurotsuki release notes](https://san-kernel.vercel.app/Release/kurotsuki-r1.1.107).
- The ramdisk's `fstab.qcom` declares `system`, `vendor`, `product`, `system_ext`, and `odm` as logical, first-stage-mounted ext4 partitions. It contains no `cust` mount.
- `dtbo.img` has the expected DTBO magic. `vbmeta.img` has an `AVB0` header and flags value `3`, meaning hashtree and verification are disabled in that vbmeta image.

These checks make a malformed boot header, a missing `cust` fstab entry, and ordinary AVB verification failure less likely as the immediate cause. They do not prove the kernel successfully boots on the physical device.

## Product-partition completeness concern

The dynamic-partition operation list removes existing groups and says it is applying a full OTA, then creates a `product` logical partition of 2,504,368,128 bytes. But the product transfer list only writes 123,902 blocks (507,502,592 bytes), contains no `zero` or `erase` commands, while the reconstructed product ext4 superblock declares 601,750 blocks (2,464,768,000 bytes).

That means the archive does not explicitly populate the whole product filesystem. The cloud reconstruction confirms this is not a complete product filesystem: `e2fsck -fn` reports invalid extent nodes and a corrupted root directory, and the product mount exposes no files. The installer first discards the physical `system`/super storage and then rebuilds dynamic-partition metadata, so those untouched blocks cannot be relied on to survive. This is a **confirmed packaging defect and a leading explanation for the failed boot**. The exact repair requires the complete product source image/tree; the partial `.new.dat.br` payload cannot recreate the missing blocks by itself.

## Cloud reconstruction results (2026-10-18)

The transfer-list reconstruction and read-only filesystem checks completed in GitHub Actions. This is a real filesystem check of the archive's payload, not a boot simulation.

| Partition | Reconstructed ext4 result | Interpretation |
| --- | --- | --- |
| system | All five e2fsck passes completed without reported errors | Filesystem structure appears consistent |
| vendor | All five e2fsck passes completed without reported errors | Filesystem structure appears consistent |
| system_ext | All five e2fsck passes completed without reported errors | Filesystem structure appears consistent |
| odm | All five e2fsck passes completed without reported errors | Filesystem structure appears consistent |
| product | e2fsck aborted after invalid extent nodes and a corrupted root directory; mounted tree was empty | **Broken/incomplete product payload confirmed** |

The archive does contain `Settings.apk` and `SystemUI.apk`, both located under `system_ext/priv-app`, plus `SettingsProvider.apk` under `system/priv-app`. The resource dump identifies Android 15-era Material 3 and Quick Settings resources, including `qs_tile_ripple_color`, `brightness_slider_overlay_color`, and several `m3_sys_color_*` colors. The captured SystemUI overlayable-policy dump is empty, so do not assume those resources are legally overlayable just because they exist in the APK. Any SystemUI RRO must be validated against the actual target policy.

The product issue is now the first packaging blocker to resolve before another ROM flash. The overlay and wallpaper work must not be allowed to obscure this bootability problem.

## What this says about the cust theory

The ZIP does not attempt to create or flash `cust`. That alone does **not** prove that cust is irrelevant to boot, because a device/vendor configuration can expect files or properties from a separate cust partition. But there is no direct evidence in the installer that a missing cust image caused the fastboot return.

## Most useful next checks

1. Inspect the actual boot image header and embedded kernel/ramdisk sizes.
2. Inspect `vbmeta.img` magic, AVB flags, and descriptors.
3. Compare the OTA's expected source/target metadata and transfer-list versions.
4. Inspect recovery logs for the final successful flash messages and any image verification errors.
5. Verify the target's retrofit-dynamic-partition configuration and boot image compatibility before modifying anything.

**Do not flash a replacement image based on this preliminary report.** The ZIP's integrity test passing only verifies the archive's compressed data, not Android bootability.
