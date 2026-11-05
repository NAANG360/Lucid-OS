# Agent instructions — Lucid OS

**Target:** Xiaomi Redmi Note 7 / Note 7S (`lavender`, Snapdragon 660). **ROM direction:** Android 15.

## Current ROM state

- Continue work on `feature/lucid-control-centre-connect`.
- The current published ROM is **LucidOS Android 15 lavender Build 4 experimental**, version `0.4-dev`, release tag `lucid-android15-lavender-build-4`.
- Public ROM asset: [LucidOS-Android15-lavender-build-4-experimental.zip](https://github.com/NAANG360/Lucid-OS/releases/download/lucid-android15-lavender-build-4/LucidOS-Android15-lavender-build-4-experimental.zip).
- Published asset identity: **1,355,867,286 bytes**, SHA-256 `d2b81388a57c685b82e3cb8f29da475f540dfcb9f5114003846d9337533d3219`.
- Build 4 was produced by taking the published Build 3 installer/archive as its base and overlaying Lucid companion apps, desktop payload, and wallpaper assets. Preserve the original Build 3 release; publish future experimental integrations as separate releases.
- The base archive's ancestry and Android identity are not fully established. Its Android 15 SDK metadata conflicts with an Android 10 build fingerprint. Do not state a more specific upstream base identity unless new evidence verifies it.
- Build 4 is an experimental package. Its release notes state that it has **not been physically boot-tested**. Never describe it as confirmed bootable or safe to flash based on CI packaging, ZIP integrity, checksums, or filesystem checks alone.

## Build and integration rules

- The ROM build is the primary deliverable. Use `.github/workflows/build-integrated-lucid-rom.yml` as the current integrated packaging route; inspect its current contents and latest run before changing it.
- Build from the verified published ROM archive and overlay only deliberate, maintained Lucid additions. `system-tree/system/` is an older extracted snapshot for reference/comparison, not a replacement system image.
- Do not rebuild, replace, reconstruct, or request uploads for the product partition. Do not restore obsolete product-only reconstruction workflows.
- The current Build 4 integration workflow packages these companion apps as regular system apps: Lucid Control Centre, Settings, Launcher, Updater, Wallpapers, Desktop, and Dynamic Island. Do not imply these apps replace native Android SystemUI.
- The actual native SystemUI source has not been rebuilt in the published Build 4. The Developer Options root toggle is also not integrated. Treat both as outstanding work until source changes and validation prove otherwise.
- Do not copy stale setup/firstboot flows or package references to components that are not present and maintained in the current tree. Verify package IDs and source before integration.
- Include desktop scripts, the Ubuntu rootfs archive, and wallpaper assets only when their current source, purpose, and packaging are verified. Preserve user-supplied artwork, especially the bubbles wallpaper; do not substitute generic artwork or generic UI templates.
- Preserve Lucid's established dark surfaces, restrained lavender/violet accents, and intentional visual language.

## OTA and release rules

- `ota/stable.json` must point to the **ROM ZIP**, never the updater APK or updater recovery installer. Keep its URL, SHA-256, byte size, version, and changelog synchronized with the exact public ROM asset.
- The current manifest identifies Build 4 as `0.4-dev`, with the Build 4 ROM URL, SHA-256, and byte size listed above, and currently sets `installable: true`. This metadata value is not evidence of a successful physical-device install or boot. Reassess it whenever compatibility evidence changes; do not leave it true if installer compatibility is unverified.
- The updater recovery ZIP installs the updater app; it is an add-on package, not a ROM.
- Preserve existing public releases. Publish new experimental ROM output as a separate prerelease.
- Before publishing, verify the exact artifact, ZIP integrity, expected payloads, SHA-256, byte size, public non-draft asset, and matching OTA metadata. Keep updater assets and ROM assets clearly distinguished.

## Verification and reporting

Report each verification level separately:
1. Workflow completed.
2. ZIP integrity and expected payload checks passed.
3. Installer scripts and package metadata validated.
4. OTA metadata matches the exact public ROM asset.
5. Installation succeeds in a compatible recovery.
6. ROM boots and core hardware works on a physical `lavender` device.

Do not infer a higher level from a lower one. State the latest run, artifact, commit, and exactly what was tested. Prefer free tooling and GitHub Actions; never claim a workflow or artifact succeeded without run evidence.
