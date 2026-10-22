# Lucid-OS

Lucid OS targets the Xiaomi Redmi Note 7 (`lavender`). The current ROM effort has **moved away from the old LineageOS 22.2 reconstruction**. The user has already downloaded the selected new base ROM and has an existing LucidOS build flashed on-device. Do not ask them to download it again or treat the device as unflashed. Do not use the abandoned product-partition reconstruction as a build dependency or blocker.

## Current base-ROM direction

Use a clean Android 15 ROM ZIP for `lavender` as the new base. The current candidate recorded in project handoff is **Rising-Revived 6.3 / Android 15 / lavender (March 2025)**. This is the already-selected/downloaded base, not a request to start a new download. Continue from the existing project artifacts and the already-flashed LucidOS build; verify the precise archive and current build through repository artifacts and workflow evidence rather than resetting the project state. Never substitute a ROM for another device (for example, `violet`) even if the marketing name/version matches.

The build process must start from that new ROM archive, inventory its actual installer and partition format, then layer Lucid changes onto it. Do not reconstruct or carry forward the abandoned LineageOS product payload. Preserve the new base's boot-critical files, partition logic, kernel/vendor compatibility, and SELinux metadata unless a tested Lucid change specifically requires a modification.

## Lucid requirements

- Original Lucid/Cosmos Figma file is the only source for Lucid branding and artwork. Do not use generated, stock, or substitute wallpapers/art.
- Integrate the Lucid System Updater into the ROM itself; an add-on ZIP is only a development artifact, not the final ROM.
- Preserve existing Lucid scripts, setup app, launcher work, chroot/desktop assets, and required init hooks after auditing compatibility with the new base.
- Audit and implement the requested native UI/apps against the new ROM rather than assuming the extracted legacy system tree matches it.

## Current deliverables and verification

- `.github/workflows/lucid-system-updater.yml` builds the updater app and a recovery-flashable **add-on** package. It is not a full ROM build.
- The new base and an existing LucidOS build are already part of the project history; resume from that state and inspect the existing archive/build artifacts. Do not restart the download or old-ROM investigation.
- The user reports an existing LucidOS build is already flashed on their phone. That is distinct from claiming a newly produced artifact is verified. Continue from the actual installed baseline and repository build artifacts; only claim a *new* package is validated after its own checks pass.

## Abandoned work

The old LineageOS 22.2/product-payload reconstruction workflow and its product-partition diagnosis are historical only. They are not prerequisites for the current build and must not be used to gate progress on the new ROM base.
