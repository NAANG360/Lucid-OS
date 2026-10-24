# Project state for agents

## Target and direction

- Device: Xiaomi Redmi Note 7 / Redmi Note 7S (`lavender`), Snapdragon 660.
- ROM target: Android 15.
- Base direction: Rising-Revived 6.3 Android 15 lavender archive.
- The project is intended to become an integrated, bootable ROM. Do not confuse updater packaging or product-only reconstruction with that goal.

## Existing work

- Public repository: `NAANG360/Lucid-OS`.
- The updater app is built through `.github/workflows/lucid-system-updater.yml`.
- The recovery updater installer is published to the Android 15 lavender Build 3 release.
- OTA metadata is stored in `ota/stable.json` and should identify the ROM ZIP, not the updater installer.
- Existing public Build 3 ROM archive: `LucidOS-Android15-lavender-unsigned.zip`.
- A recovery installer ZIP being publicly available does not establish that it has been tested on every recovery or that a ROM can boot.

## Known verification boundary

Treat each check separately:
1. Workflow completed.
2. ZIP archive passes integrity checks.
3. Payload and installer scripts are present and valid.
4. OTA manifest matches the exact public ROM asset's checksum and byte size.
5. Installer succeeds on a compatible recovery.
6. ROM boots and core hardware works on a physical lavender device.

Only report the highest level actually demonstrated. Do not infer steps 5 or 6 from CI alone.

## Working method

- Inspect current branches, files, and workflow logs before acting.
- Prefer end-to-end fixes and runnable validation over documentation-only progress.
- Keep technical handoff notes here, not in the public README.
- Never reintroduce the abandoned LineageOS 22.2 product reconstruction as a prerequisite.
