# Lucid Updater

Native Java OTA client for `lavender`. It reads `ota/stable.json`, validates the target device, HTTPS URL, archive size and SHA-256, downloads to app-owned storage, and exposes a recovery handoff only after verification.

The client refuses a manifest with `installable: false`. The currently published Android 15 build is intentionally blocked because the repository's audit found an unresolved SDK 35 / Android 10 fingerprint mismatch and no verified physical-device boot. The OTA CI checks the manifest's URL, size, and SHA-256 against GitHub's release metadata; this is not a bootability test.

Recovery staging requires an already configured root provider and a compatible TWRP/OrangeFox build that supports the generated OpenRecoveryScript. This app does not create Android root, replace recovery, or prove that an arbitrary ROM archive is flashable. No update is silently installed or data-wiped.