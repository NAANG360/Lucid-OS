# ROM base findings (preliminary)

## Evidence from the committed tree

The current `system-tree/system/build.prop` contains these conflicting identifiers:

- `ro.build.version.release=15`
- `ro.build.version.sdk=35`
- `ro.lineage.version=22.2-20251222-UNOFFICIAL-lavender`
- Build fingerprint: `xiaomi/lavender/lavender:10/QKQ1.190910.002/V12.5.3.0.QFGCNXM:user/release-keys`

This means the fingerprint's vendor/base label says Android 10 while the declared system build is Android 15. Do not use the fingerprint alone to choose an overlay target or conclude that the system is Android 10.

The committed extracted tree currently has `system/framework/framework-res.apk`, but no `Settings.apk` or `SystemUI.apk` at the expected `system/priv-app/Settings/` and `system/priv-app/SystemUI/` paths. The release ZIP is the authoritative next source for package and installer inventory.

## Release archive

- Release tag: `tobelucid-source-v1`
- Asset: `LucidOS-unsigned.zip`
- The archive is explicitly unsigned and has not been verified as bootable.
- The draft release has an asset attached. The audit workflow must locate draft assets via the Releases API rather than the tag-based `gh release view` command, which returns “release not found” for this draft.

## Safety for the next build

Do not flash new system, boot, vbmeta, super, or cust images based only on this report. First inspect the actual installer, boot image metadata, dynamic-partition configuration, SELinux/file-context handling, and the target framework resource table. Keep any theme overlay separately installable and reversible.
