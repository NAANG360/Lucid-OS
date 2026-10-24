# Repository notes for coding agents

Keep this file short. The detailed working notes live in `agents/`; the public-facing project overview is in `README.md`.

## Ground rules

- Target device: Xiaomi Redmi Note 7 / Note 7S, codename `lavender`. Never substitute another device's ROM.
- Current ROM direction is Android 15 using the selected Rising-Revived lavender base. The abandoned LineageOS 22.2 product reconstruction is historical, not a build dependency.
- Make changes in the repository and validate them. A diagnosis or journal update alone is not a completed task.
- Do not claim an artifact installs or boots unless the relevant test actually happened. CI success, ZIP integrity, checksum verification, recovery compatibility, and physical-device boot are different things.
- The OTA manifest must point to the **ROM ZIP**, not the updater APK or updater recovery installer. Keep URL, SHA-256, byte size, version, and changelog in sync with the actual public asset.
- The updater recovery ZIP is an add-on installer for the updater app; it is not a ROM.
- Preserve boot-critical base files, partition logic, vendor/kernel compatibility, and SELinux metadata unless a tested change requires otherwise.
- Keep user-facing prose natural and specific. Release notes should describe changes and known limitations, not generic hype.
- Never commit keys, tokens, private signing material, or device-specific secrets.

## Start here

1. Read `agents/PROJECT_STATE.md` for the current technical direction and known caveats.
2. Read `agents/RELEASES.md` before touching OTA metadata or release publishing.
3. Inspect the relevant workflow and its latest run before changing CI.
4. After changes, report the commit, artifact/run link, and exactly what was verified.
