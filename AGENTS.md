# Agent instructions — Lucid OS ROM integration

**Target:** Xiaomi Redmi Note 7 / Note 7S (`lavender`, Snapdragon 660).

## Current user-selected build route

- The user explicitly selected the published LucidOS Build 3 archive as the system-image source for the next experimental integration build. Use `.github/workflows/build-integrated-lucid-rom.yml` to download and verify that exact archive, reconstruct its system partition, overlay the checked-in Lucid additions, repack the system payload, and produce a public experimental Build 4 release.
- Build 3 asset identity: `LucidOS-Android15-lavender-unsigned.zip`, 1,326,072,199 bytes, SHA-256 `c7262b06d6c2a60ceae395b21539de972c3c52085640bee87a7a55dacab7aba7`, release tag `lucid-android15-lavender-build-3`.
- This route is an experimental repack of Build 3. Do not claim the archive's upstream ancestry is proven: the Android 15 SDK metadata versus Android 10 fingerprint discrepancy remains unresolved.
- Do not use the historical `tobelucid-source-v1/LucidOS-unsigned.zip` or the Lineage-named `lineage-22.2-20251222-UNOFFICIAL-lavender.zip` as a base. Do not relabel LineageOS as RisingOS Revived.
- `system-tree/system/` is an older extracted snapshot used for comparison/reference only. It is not the source system image for the integrated build. Never replace Build 3 wholesale with this snapshot; overlay only deliberate Lucid additions from tracked source and app projects.
- The old `LucidSetup.apk`/firstboot flow that launches `com.cosmos.setup` is stale and must not be copied into the new image unless a matching, maintained app/package is verified.

## Product partition ban

- Do not rebuild, replace, reconstruct, or request uploads for the product partition.
- Do not restore obsolete product-only rebuild/audit workflows. The goal is an integrated ROM package based on Build 3's system partition, not a product-only archive.

## Integration and safety rules

- Build Lucid app APKs in CI and package them as regular system apps unless their signing/privileged-permission requirements are actually satisfied. Do not pretend companion apps replace real Android SystemUI integration.
- Include the maintained desktop scripts, Ubuntu rootfs archive, and tracked wallpaper assets only after the system image is reconstructed. Do not include stale setup flows or enable automatic privileged execution.
- Keep the OTA manifest URL, SHA-256, size, version, and changelog synchronized with the actual published ROM asset. Keep `installable: false` until package compatibility and installation are proven.
- Preserve the original Build 3 release; publish the integrated output as a separate prerelease.
- Never label an archive bootable/installable based solely on ZIP integrity, SDK metadata, filesystem checks, or CI packaging. Physical-device boot and hardware smoke tests remain unverified until performed on a compatible lavender device.

## Execution rules

- Continue on `feature/lucid-control-centre-connect`.
- Prefer free tooling and GitHub Actions; do not claim a workflow or artifact succeeded without its run evidence.
- The ROM build is the primary deliverable. Standalone APK builds, audits, and reports are supporting steps, not substitutes for the integrated ROM.
- Preserve Lucid's existing dark surfaces, restrained lavender/violet accents, supplied artwork, and the bubbles reference. Do not replace the design language with generic templates.
