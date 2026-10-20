# Lucid-OS Engineering Journal

Updated: 2026-10-20 (UTC)

## Objective and acceptance criteria
Deliver a complete, installable Lucid-OS ROM for Xiaomi Redmi Note 7 (`lavender`) based on a compatible existing Android ROM, with intended Lucid components integrated and preserved. Acceptance requires: (1) verified base/device compatibility, (2) reproducible packaging with correct installer and partition layout, (3) integration of required Lucid system/UI/setup components, (4) static package/image checks and tests passing, (5) eventual device boot validation or a precise documented external blocker. A green workflow alone is not acceptance.

## Repository state
- Repository: `NAANG360/Lucid-OS` (confirmed public)
- Working branch: `fix/chroot-desktop-runtime`
- Default branch: `main`
- Current latest engineering commit at journal update: `b79ab7d2d6cec463a4b1725fea5cec5f3d4d54c4`
- Candidate base under investigation: Rising-Revived 6.3 Android 15 vanilla for lavender, archive filename `RisingOS_Revived-6.3-250319-0738-VANILLA-COMMUNITY-lavender.zip`.
- Existing Lucid release asset `LucidOS-unsigned.zip`, immutable asset ID `652845990`, pinned SHA-256 `6e9fa4bc2c7dffee9671a02f45bf3b4b7f881b33546cde33b2ea2c9f8ca953b1`.

## Latest verified failures and fixes
1. Runs before public repo / billing fix had jobs with runner_id 0 and no steps. User's GitHub screenshot confirmed Actions blocked because recent payments failed or spending limit was too low. After repo was made public, runner provisioning started and steps executed.
2. Run `39379889888` (commit `a6acc84`) ran through checkout and preservation tests but failed in the base download/inspection step with `Invalid ZIP filename.` Actual logs showed `BASE_ROM_URL:` and `EXPECTED_FILENAME:` empty on push trigger. Later enforcement step also failed because no product validation result existed; that was a secondary cascade, not root cause.
3. Commit `b79ab7d2d6cec463a4b1725fea5cec5f3d4d54c4` fixes push-trigger defaults for base URL and expected filename.
4. At journal update, run `39383327754` for commit b79ab7d is in progress. Its steps: checkout passed, preservation-manifest unit tests passed, preservation snapshot passed, base download currently running. Do not assume result until logs are polled.

## Broader known evidence (not necessarily root causes)
- Existing Lucid installer archive has a block-based incremental OTA with system/vendor/product/odm/system_ext transfer lists, `super_dummy.img`, and updater logic requiring retrofit dynamic partitions.
- It flashes boot.img, dtbo.img and vbmeta.img. No cust image or cust mount in the examined ramdisk.
- Boot image header is structurally plausible; kernel string `4.19.325-st-San-Kernel-Kurotsuki-R1.1.107` is reported by prior audit as lavender-targeted. This does not prove successful boot.
- Ramdisk fstab declares logical system/vendor/product/system_ext/odm ext4 and /data F2FS with file-based encryption/inline crypto. Data filesystem mismatch is a candidate only if device state indicates it.
- Extracted system tree's build.prop combines Android 15 SDK/release declarations with an Android 10 fingerprint string; do not infer OS compatibility from fingerprint alone.
- Expected Settings/SystemUI paths are missing from the tracked extracted tree; actual archive inventory must be authoritative.
- Repo contains setup app source, privileged APK, init hooks, desktop scripts, Ubuntu userspace, themes and preservation manifest. Must distinguish tracked/source artifacts from packaged/runtime functionality.
- Privileged desktop/chroot path is intentionally fail-closed; property gates do not make a chroot a security boundary. Keep unrelated privilege feature work subordinate to bootable ROM objective.

## Current hypothesis
The immediate blocker in the clean-base audit was missing workflow input defaults, now corrected. Next determine whether the selected SourceForge URL resolves to the actual archive and whether its filename/content matches expectations. Then use the resulting archive metadata/installer/boot evidence to assess overall compatibility. In parallel, inspect existing installer and boot validation reports to identify non-product blockers; do not let product filesystem reconstruction become the sole objective.

## Next actions
1. Poll run `39383327754` and inspect exact logs/artifacts after completion.
2. If download fails, determine actual SourceForge file URL/redirect/filename and correct the workflow, then rerun.
3. Once archive is available, compare its metadata, installer, partition layout, boot/ramdisk and framework against Lucid's current release and device requirements.
4. Update this journal with exact commit IDs, log evidence, tests and next action after each meaningful iteration.
5. Produce an integrated candidate ROM only after establishing what base package and installer actually support; test archive integrity and image/layout constraints before claiming completion.

## Status labels
- Verified: directly observed in repository metadata, workflow logs, or executed tests.
- Partially verified: structural/static evidence only.
- Unverified: no execution/device evidence.
- Blocked: requires unavailable hardware/permissions/data after available alternatives have been exhausted.

## Completion status
NOT COMPLETE. No boot-verified ROM exists yet. Device-level boot test remains unverified.


## Verified update — 2026-10-20 UTC (run 39383327754)
- Repository metadata confirms visibility `public`; default branch is `main`. Work branch remains `fix/chroot-desktop-runtime`.
- Clean Android 15 Base Audit run `39383327754` completed successfully at commit `b79ab7d2d6cec463a4b1725fea5cec5f3d4d54c4`.
- Job `118497240160`: checkout, preservation-manifest test, preservation-payload snapshot, base download, product reconstruction, evidence upload, and candidate gate all completed successfully.
- Selected archive filename: `RisingOS_Revived-6.3-250319-0738-VANILLA-COMMUNITY-lavender.zip`.
- Reconstructed product image identified by `file` as ext2/3/4-family filesystem with volume name `product`; SHA-256 `45e4ebb2020584d772e9f5d642ec8510a24610407fe56ad224cd908d7de85e9a`; read-only `e2fsck -fn` exit code 0. This verifies product-image reconstruction/integrity only, not ROM compatibility or bootability.
- Evidence artifact: `lucid-clean-base-intake`, artifact ID `12246961907`, 11,576 bytes, SHA-256 `4187e72136b958db4395253b815c1c81a2549bd42b1a5f27a21e3479eda8cded`; available from run `39383327754`.
- The earlier run `39379889888` failed; later rerun `39383327754` is the current successful audit result. Privilege-boundary guardrails succeeded at `b0fba1d7e66da85cff9bdf258a81bf179835e124`.
- Important scope correction: product validation is one diagnostic gate, not the objective. Next prioritize auditing the whole base ZIP (boot/vendor/system/system_ext/odm, installer, dynamic-partition metadata, kernel/ramdisk/fstab and device compatibility) and then integrate the full Lucid-OS component set into a reproducible complete ROM package. Do not treat a passing product check as acceptance.
- Unresolved: no complete integrated Lucid-OS ZIP has been built and no physical-device boot test has been performed.


## Verified continuation — 2026-10-20 (live archive/log inspection)
- Re-queried repository metadata and Actions: repository is public; default branch `main`; work branch `fix/chroot-desktop-runtime`. Journal blob was fetched live and contained the prior run record. Latest relevant successful clean-base audit remains run `39383327754`, commit `b79ab7d2d6cec463a4b1725fea5cec5f3d4d54c4`; job `118497240160` succeeded. Artifact `lucid-clean-base-intake` ID `12246961907` exists, not expired.
- Downloaded the audit job logs and artifact. The real base ZIP download succeeded with HTTP 200; effective SourceForge URL was `https://master.dl.sourceforge.net/project/termux-lavender/Rising-Revived/15.0/RisingOS_Revived-6.3-250319-0738-VANILLA-COMMUNITY-lavender.zip?... `; size 1,404,508,858 bytes; SHA-256 `2f105eadd8ea0a044a4930cf60a83d4c91983a500c2a97125d45a4cb8b6e4f2c`; `unzip -tq` passed.
- Archive metadata directly observed: `pre-device=lavender`, `post-sdk-level=35`, `post-security-patch-level=2025-03-05`, but `post-build=xiaomi/lavender/lavender:10/QKQ1.190910.002/V12.5.3.0.QFGCNXM:user/release-keys`. This is a significant identity mismatch between Android 15 SDK metadata and Android 10 Xiaomi fingerprint; not by itself proof of an invalid ROM, but must be resolved by inspecting actual partition build.prop and installer/build identity before acceptance.
- Base archive entry inventory is only 29 entries. It has `boot.img`, `vbmeta.img`, `dynamic_partitions_op_list`, and system/vendor/product transfer payloads, but **does not contain `dtbo.img`**. The existing LucidOS archive does contain `dtbo.img`, but lacks the base's `recovery.img` and `install/bin/flash_super_dummy.sh`. The candidate's actual updater script mentions lavender and retrofit dynamic partitions. These differences make installer/boot-component reconciliation a high-priority requirement; do not blindly transplant the old installer or assume the old DTBO is compatible.
- The archive's metadata proves device label and SDK-level declaration only, not vanilla status or runtime compatibility. The workflow's `vanilla_status` is explicitly “not provable from archive entry names”. Product filesystem SHA-256 and read-only `e2fsck -fn` exit code 0 remain a narrow filesystem result.
- Root pipeline gap confirmed from live source: `.github/workflows/rebuild-product-full.yml` rebuilds only product and starts from the existing LucidOS recovery ZIP; it does not integrate system-tree changes into system.img. `docs/ROM_INTEGRATION_STATUS.md` acknowledges this. `tools/stage_lucid_overlays.py` is a dry-run/apply overlay copier only; it does not build ext4, set Android ownership/SELinux labels, repack OTA transfer payloads, reconcile installer metadata, or assemble a new base-derived full ROM. Thus no current workflow is a complete clean-base-to-LucidOS ROM pipeline.
- The clean-base audit run's successful status is confirmed; it must not be interpreted as acceptance. No complete integrated candidate archive has been produced, no target system/vendor/odm/system_ext contents have been extracted and compared in this session, and no boot or device test has occurred.
- Next action: implement a dedicated end-to-end *base package inspection* stage in the audit workflow that captures the exact base updater script, dynamic partition operation list, complete entry list, boot/vbmeta metadata, extracted system/vendor build.prop identity and presence of critical images; explicitly flag absent DTBO and Android-10 fingerprint/SDK-35 mismatch. Use evidence to decide whether this base is accepted or reject it in favor of the documented June 2025 LineageOS 22.2 candidate. After that, build a real base-derived integration pipeline rather than extending the product-only workflow.
