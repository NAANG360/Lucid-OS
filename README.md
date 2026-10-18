# Lucid-OS

Private development repository for Lucid OS targeting the Xiaomi Redmi Note 7 (`lavender`).

## Project status

This repository currently contains an extracted Android system filesystem and related reconstruction work. It is **not yet a complete, independently buildable ROM source tree**, and the unsigned release archive has not been verified as bootable.

## Work areas

- `system-tree/` — extracted Android system filesystem.
- `system_original_metadata.tsv` — original filesystem metadata, if available.
- `design/LUCIDOS_VISUAL_SYSTEM.md` — Figma-to-Android visual implementation map.
- `docs/ROM_BASE_FINDINGS.md` — evidence-based notes about the current ROM base.
- `docs/ROM_INSTALLER_FINDINGS.md` — block-transfer, boot-image, AVB, and product-filesystem findings.
- `.github/workflows/rom-cloud-audit.yml` — cloud inventory of the unsigned release archive.
- `.github/workflows/rom-reconstruct-partitions.yml` — reconstructs OTA block images in a cloud runner and inventories the actual Settings/SystemUI/launcher APKs.
- `.github/workflows/lucid-theme-overlay.yml` — builds the LucidOS Material color resource overlay, wallpaper app, and boot animation.
- `.github/workflows/rebuild-product-full.yml` — rebuilds the product partition as a complete filesystem and produces a self-contained full product payload.
- `docs/PRODUCT_REBUILD_INPUT.md` — explains the one required source-tree upload for repairing the incomplete product payload.
- `theme/assets/` — selected original Figma wallpaper assets (dusk default candidate and alpine alternate).
- `theme/scripts/generate-palette.mjs` — generates the 65 Android tonal palette resources from the Figma-inspired blue source color.

## Design direction

Use the LucidOS designs in the [Figma file](https://www.figma.com/design/UPUZotxcIfcIdElWklOHFN/cosmosOS) as the visual source, while translating the screens into native Android behavior. Do not ship cosmosOS wordmarks or iPhone mockup frames as ROM assets.

## Device and build constraints

The committed build properties declare SDK 35 / Android 15 and LineageOS 22.2 while retaining an Android 10 base fingerprint. That mismatch must be resolved from the actual release archive before targeting overlays or rebuilding images.

## Build and safety status

**Current confirmed blocker:** the product OTA payload writes only 123,902 blocks, but the product filesystem spans 601,750 blocks. Reconstructing it from a clean super image produces an ext4 filesystem with invalid extents and a corrupt root directory. The current unsigned OTA is therefore not a self-contained full install and should not be reflashed as-is. See `docs/ROM_INSTALLER_FINDINGS.md` and `docs/PRODUCT_REBUILD_INPUT.md`.

The framework palette overlay and wallpaper app compile in cloud CI. Runtime framework overlay policy is not yet proven against the custom-signed ROM, and the full product rebuild still requires the complete `product_mod` tree. No artifact should be described as bootable until the rebuilt product filesystem, installer, partition metadata, image contents, and relevant boot configuration have been checked. Prefer reversible overlays and cloud validation before modifying boot-critical images.
