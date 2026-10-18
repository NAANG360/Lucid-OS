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

## What this says about the cust theory

The ZIP does not attempt to create or flash `cust`. That alone does **not** prove that cust is irrelevant to boot, because a device/vendor configuration can expect files or properties from a separate cust partition. But there is no direct evidence in the installer that a missing cust image caused the fastboot return.

## Most useful next checks

1. Inspect the actual boot image header and embedded kernel/ramdisk sizes.
2. Inspect `vbmeta.img` magic, AVB flags, and descriptors.
3. Compare the OTA's expected source/target metadata and transfer-list versions.
4. Inspect recovery logs for the final successful flash messages and any image verification errors.
5. Verify the target's retrofit-dynamic-partition configuration and boot image compatibility before modifying anything.

**Do not flash a replacement image based on this preliminary report.** The ZIP's integrity test passing only verifies the archive's compressed data, not Android bootability.
