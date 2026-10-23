# LucidOS ROM integration status — 2026-10-20

## Integrated into the product-image build

The full-product rebuild workflow now builds `com.lucid.os.about` from `theme/about-app`, verifies its APK signature and package metadata, places it at `/product/app/LucidAbout/LucidAbout.apk`, then extracts that exact APK back out of the rebuilt ext4 product image and compares the bytes before producing the full-block product OTA payload.

When the workflow completes successfully, the output is the original LucidOS recovery ZIP with a complete rebuilt `product` payload and the About LucidOS app included. It remains unsigned at the ROM ZIP level, matching the existing release's unsigned status. The APK's development signing key is generated during the build and is not a stable release key; treat it as a development/preinstalled app, not an app intended for future signed updates.

## Inputs still required for the flashable ROM build

The workflow intentionally requires a complete product filesystem tree, either as `product-mod-tree.tar.zst` attached to release `tobelucid-source-v1`, or recoverable and verifiable from the original LineageOS archive. The repository does not currently contain that full extracted tree. The partial `product.new.dat.br` is not a valid substitute. The workflow must stop rather than create a potentially corrupt product partition.

## Privilege isolation is not yet release-ready

The current repo contains a partial `system-tree`, but the existing full-product workflow does not rebuild the system partition. Changes under `system-tree/system/etc/init` and `system-tree/system/bin` therefore are not automatically present in the ROM ZIP produced by that workflow.

The branch adds fail-closed checks to the source tree, but no real Developer Options control or audited host-root isolation backend exists. Do not set `persist.lucid.chroot_backend_ready=1` or ship an init service running as Android-host root. A chroot by itself does not isolate host root. The Settings/framework source needed for a genuine Developer Options entry is absent from this repository snapshot.

## Flashing status

No flashable ROM artifact is claimed until the full-product workflow has completed and uploaded `LucidOS-unsigned-full-product.zip`. Even then, the package is unsigned and has not been validated on a physical Redmi Note 7. Keep a known-good recovery and ROM available before testing any build.
