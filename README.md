# Lucid-OS

Lucid OS development repository targeting the Xiaomi Redmi Note 7 / Note 7S (`lavender`, Snapdragon 660), with RisingOS Revived as the intended upstream base.

## Source-of-truth and current status

- The latest published Lucid ROM to inspect is [build 3](https://github.com/NAANG360/Lucid-OS/releases/tag/lucid-android15-lavender-build-3), asset `LucidOS-Android15-lavender-unsigned.zip` (1,326,072,199 bytes).
- The build-3 archive's exact upstream ancestry has not yet been proven from its internal metadata. Do not label it a verified RisingOS-based or bootable ROM until the evidence establishes that.
- `system-tree/system/` is the existing extracted system tree and is the candidate to preserve. Verify it against build 3 using `.github/workflows/verify-system-tree-build3.yml`; do not replace it with a potentially older snapshot just because the archive contains one.
- `AGENTS.md` defines the authoritative source rules and excludes historical ROM archives.
- The OTA manifest currently points at build 3 but marks it non-installable pending compatibility and device verification.

## Main work areas

- `system-tree/` — existing extracted Android system filesystem and related assets.
- `theme/` — Lucid UI, native app projects, original visual assets, and theme resources.
- `design/` — Lucid visual-system references.
- `agents/FEATURE_INTEGRATION_PLAN.md` — feature integration checklist.
- `.github/workflows/verify-system-tree-build3.yml` — downloads build 3, verifies its published size/digest, inspects the archive, reconstructs the system partition when possible, and compares file hashes against the existing extracted tree.
- `.github/workflows/rom-cloud-audit.yml` and `.github/workflows/rom-reconstruct-partitions.yml` — legacy audit workflows; their old archive source references must be corrected before their results are used.
- `ota/stable.json` — current OTA manifest.

## Explicitly out of scope

**Do not rebuild or replace the product partition.** The previous product-partition rebuild workflow and its source-upload instructions are obsolete and must not be used. The goal is to verify and use the existing extracted system tree, then continue toward an integrated ROM—not a product-only payload.

## Design direction

Preserve Lucid's own dark, restrained violet identity and original artwork. Keep standard Android behavior functional and avoid substituting generic visual styles.

## Safety and verification

ZIP integrity, partition filesystem checks, successful APK builds, or CI packaging do not prove device bootability. Keep the OTA marked non-installable until archive metadata, partition compatibility, integration, and physical-device boot are verified.
