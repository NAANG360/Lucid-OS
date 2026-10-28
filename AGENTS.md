# Agent instructions — Lucid OS ROM source authority

**Target:** Xiaomi Redmi Note 7 / Note 7S (`lavender`, Snapdragon 660).

## Non-negotiable source-of-truth rules

1. **Do not use the historical `tobelucid-source-v1` release asset `LucidOS-unsigned.zip` as a ROM base or build input.** It is explicitly obsolete and is retained only as historical evidence. Do not build from it, reconstruct partitions from it, or treat findings derived from it as authoritative for the current base.
2. **Do not use the draft `original-product-analysis` asset `lineage-22.2-20251222-UNOFFICIAL-lavender.zip` as the base.** Its filename identifies LineageOS and does not establish it as the intended RisingOS Revived base. Do not silently relabel it as RisingOS.
3. **Do not treat earlier installer/partition audit reports as facts about the current base** unless each finding is re-established against the selected current archive. Mark legacy findings as historical or superseded.
4. The latest published Lucid ROM asset currently identified is:
   - Release: `lucid-android15-lavender-build-3`
   - Asset: `LucidOS-Android15-lavender-unsigned.zip`
   - Size: `1,326,072,199` bytes
   - SHA-256 published by GitHub: `c7262b06d6c2a60ceae395b21539de972c3c52085640bee87a7a55dacab7aba7`
   - URL: `https://github.com/NAANG360/Lucid-OS/releases/download/lucid-android15-lavender-build-3/LucidOS-Android15-lavender-unsigned.zip`

   This is the newest **Lucid output archive to inspect**, not proof of the upstream base. Its RisingOS Revived ancestry remains unverified until the archive is unpacked and its metadata, build properties/fingerprint, OTA metadata, partition payloads, boot/dtbo/vbmeta images, and embedded installer are inspected.
5. The intended upstream base is **RisingOS Revived**, not LineageOS. Do not claim that the latest Lucid archive is RisingOS-based until concrete archive evidence supports that conclusion. If the latest archive does not contain enough evidence to identify the base, stop and ask for the authoritative RisingOS Revived archive/source location rather than guessing.
6. Do not delete old releases, reports, or source artifacts just to clean up history. Label them as prohibited historical inputs and keep the investigation reproducible.

## System-tree authority and explicit product-rebuild ban

- **Do not rebuild, replace, reconstruct, or request uploads for the product partition.** The former product-rebuild workflow and its input instructions are obsolete and must not be revived. Do not make a product-only archive and do not use product-rebuild logic as the route to the ROM goal.
- Use the already extracted `system-tree/system/` as the candidate system tree. Verify it against the system partition from the latest published build-3 ROM using `.github/workflows/verify-system-tree-build3.yml`.
- If comparison shows only build-3-only/newer paths while the checked-in extracted tree contains the more complete/newer files, keep the checked-in extracted tree; do not replace it wholesale with an older partition snapshot. If the comparison shows same-path content differences or an incremental system payload that cannot be reconstructed independently, inspect evidence and stop short of overwriting anything until the differences are understood.
- Preserve comparison reports as workflow artifacts. Never assume the tree is newer merely because it is checked into Git.

## Required investigation before modifying ROM packaging or OTA

- Inspect the latest published Lucid ROM ZIP in CI/cloud storage; do not commit the 1.3 GB binary to the git tree.
- Produce a reproducible inventory including ZIP integrity, complete entry list, metadata (`META-INF/com/android/metadata` and updater scripts), `system/build.prop`/equivalent properties, fingerprints and SDK, transfer lists and payload sizes, partition operation list, and boot/dtbo/vbmeta presence and hashes.
- Compare that evidence with `system-tree/`, `ota/stable.json`, the release asset's GitHub size/digest, and every workflow that currently hardcodes `tobelucid-source-v1` or the Lineage-named ZIP.
- Treat the checked-in `system-tree/` as an extracted filesystem snapshot of uncertain ancestry until proven against the selected archive.
- Do not rewrite base labels, OTA metadata, installer scripts, or release claims merely to make names consistent. First establish the actual source and compatibility; then update all affected references together.
- Never label an archive bootable or installable based solely on ZIP integrity, SDK metadata, filesystem checks, or CI packaging. Physical-device boot remains unverified until actually tested.

## Project execution rules

- Continue on the existing working branch `feature/lucid-control-centre-connect`; do not restart the project or switch to an unrelated ROM.
- The goal is a genuinely integrated Lucid OS ROM, not only standalone APKs or a product-only rebuild.
- Preserve Lucid's established design identity and original artwork.
- Prefer free tools and GitHub Actions; do not claim a workflow ran or an artifact was verified unless there is evidence from its run/report.
