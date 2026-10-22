# Lucid-OS

Lucid OS targets the Xiaomi Redmi Note 7 (`lavender`). The current ROM effort has **moved away from the old LineageOS 22.2 reconstruction**. Do not use the abandoned product-partition reconstruction as a build dependency or blocker.

## Current base-ROM direction

Use a clean Android 15 ROM ZIP for `lavender` as the new base. The current candidate recorded in project handoff is **Rising-Revived 6.3 / Android 15 / lavender (March 2025)**. This candidate still requires a real archive download and validation before it can be treated as the accepted build input. Never substitute a ROM for another device (for example, `violet`) even if the marketing name/version matches.

The build process must start from that new ROM archive, inventory its actual installer and partition format, then layer Lucid changes onto it. Do not reconstruct or carry forward the abandoned LineageOS product payload. Preserve the new base's boot-critical files, partition logic, kernel/vendor compatibility, and SELinux metadata unless a tested Lucid change specifically requires a modification.

## Lucid requirements

- Original Lucid/Cosmos Figma file is the only source for Lucid branding and artwork. Do not use generated, stock, or substitute wallpapers/art.
- Integrate the Lucid System Updater into the ROM itself; an add-on ZIP is only a development artifact, not the final ROM.
- Preserve existing Lucid scripts, setup app, launcher work, chroot/desktop assets, and required init hooks after auditing compatibility with the new base.
- Audit and implement the requested native UI/apps against the new ROM rather than assuming the extracted legacy system tree matches it.

## Current deliverables and verification

- `.github/workflows/lucid-system-updater.yml` builds the updater app and a recovery-flashable **add-on** package. It is not a full ROM build.
- The ROM-base download and archive validation are not yet confirmed successful in this repository.
- No full Lucid ROM ZIP has been verified as bootable. Do not flash or describe any artifact as bootable until the new base ZIP, installer, partition layout, boot-critical images, integration output, ZIP integrity, and device-target assertions have passed validation.

## Abandoned work

The old LineageOS 22.2/product-payload reconstruction workflow and its product-partition diagnosis are historical only. They are not prerequisites for the current build and must not be used to gate progress on the new ROM base.
