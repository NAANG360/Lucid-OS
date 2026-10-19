# Original ROM intake and comparison

Reference base expected for this project: LineageOS 22.2 / Android 15 for Xiaomi Redmi Note 7 (`lavender`), filename `lineage-22.2-20251222-UNOFFICIAL-lavender.zip`.

## First-pass archive comparison

After downloading the original ROM asset from the GitHub draft release and placing it beside the current LUCID OS ZIP, run:

```sh
python3 tools/compare_rom_archives.py \
  lineage-22.2-20251222-UNOFFICIAL-lavender.zip \
  LucidOS-unsigned.zip \
  out/ROM_BASE_COMPARISON.md
```

The tool reads ZIP members directly without extracting the ROMs, computes SHA-256 for both archives, validates ZIP CRCs, compares key image/transfer-list entries and sizes, extracts selected build identity properties, and records installer lines related to device targeting and partition operations.

## How to interpret it

- **Do not equate a missing image with a bug automatically.** Incremental OTA packages can rely on base partition contents; the installer and transfer lists determine whether this is safe.
- A matching filename or file size does not establish matching contents. Compare hashes for the payloads that matter before assuming they are unchanged.
- The archive's `ro.build.fingerprint` may identify the vendor/base lineage rather than the Android release. Consider `ro.build.version.sdk`, `ro.lineage.version`, build properties from each partition, boot image metadata, and the updater together.
- This report does not establish bootability. Keep the current known product transfer-list completeness concern open until the original package and payload behavior are examined.
- Do not flash any candidate image until the installer's target checks, retrofit dynamic-partition setup, image integrity, AVB state, and recovery/boot logs have been checked.

## Next audit sequence

1. Verify the uploaded original asset name and SHA-256.
2. Compare the original and LUCID ZIP inventories and installer scripts.
3. Identify whether the original is a full ROM or an incremental OTA and whether the product partition is present as a complete image or delta payload.
4. Compare original and LUCID transfer lists, build properties, boot/vbmeta metadata, and logical partition sizing.
5. Repair the reproducible packaging/base dependency before attempting a device flash.
