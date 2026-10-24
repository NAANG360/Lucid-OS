# Workflow map

Before editing a workflow, open the current YAML and inspect recent runs.

- `.github/workflows/lucid-system-updater.yml` — builds the updater APK and updater add-on package. It does not build the full ROM.
- `.github/workflows/publish-updater-recovery-release.yml` — packages and publishes the recovery installer ZIP and checksum.
- `.github/workflows/assemble-integrated-rom.yml` — intended to assemble the integrated Android 15 lavender ROM from the selected base and project changes.
- `.github/workflows/publish-updated-rom-release.yml` — intended to publish a successful integrated ROM artifact and update OTA metadata.
- `ota/stable.json` — public updater metadata for the ROM package.

A green updater workflow is not a green ROM build. A successful ROM packaging run is not proof of physical-device boot.
