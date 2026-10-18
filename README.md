# Lucid-OS

Private development repository for Lucid OS targeting the Xiaomi Redmi Note 7 (`lavender`).

## Project status

This repository currently contains an extracted Android system filesystem and related reconstruction work. It is **not yet a complete, independently buildable ROM source tree**, and the unsigned release archive has not been verified as bootable.

## Work areas

- `system-tree/` — extracted Android system filesystem.
- `system_original_metadata.tsv` — original filesystem metadata, if available.
- `design/LUCIDOS_VISUAL_SYSTEM.md` — Figma-to-Android visual implementation map.
- `docs/ROM_BASE_FINDINGS.md` — evidence-based notes about the current ROM base.
- `.github/workflows/rom-cloud-audit.yml` — cloud inventory of the unsigned release archive.
- `.github/workflows/rom-reconstruct-partitions.yml` — reconstructs OTA block images in a cloud runner and inventories the actual Settings/SystemUI/launcher APKs.
- `.github/workflows/lucid-theme-overlay.yml` — builds the LucidOS Material color resource overlay against the extracted framework resources.
- `theme/assets/` — selected original Figma wallpaper assets (dusk default candidate and alpine alternate).
- `theme/scripts/generate-palette.mjs` — generates the 65 Android tonal palette resources from the Figma-inspired blue source color.

## Design direction

Use the LucidOS designs in the [Figma file](https://www.figma.com/design/UPUZotxcIfcIdElWklOHFN/cosmosOS) as the visual source, while translating the screens into native Android behavior. Do not ship cosmosOS wordmarks or iPhone mockup frames as ROM assets.

## Device and build constraints

The committed build properties declare SDK 35 / Android 15 and LineageOS 22.2 while retaining an Android 10 base fingerprint. That mismatch must be resolved from the actual release archive before targeting overlays or rebuilding images.

## Build and safety status

No artifact should be described as bootable until its installer, partition metadata, image contents, and relevant boot configuration have been checked. Prefer reversible overlays and cloud validation before modifying boot-critical images.
