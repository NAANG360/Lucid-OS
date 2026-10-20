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
