# ROM base and system-tree verification status

## Authoritative selection rules

See `AGENTS.md`. The intended upstream base is RisingOS Revived. The latest Lucid output archive to inspect is the published build-3 asset `LucidOS-Android15-lavender-unsigned.zip` (1,326,072,199 bytes; SHA-256 `c7262b06d6c2a60ceae395b21539de972c3c52085640bee87a7a55dacab7aba7`).

The build-3 asset is the inspection target, not proof by itself that the upstream base is RisingOS Revived. Historical `tobelucid-source-v1` and the draft Lineage-named archive are prohibited as current ROM inputs.

## Existing extracted system tree

The repository already contains `system-tree/system/`. Preserve this tree as the candidate source rather than rebuilding the product partition or replacing the tree blindly with archive contents.

Run `.github/workflows/verify-system-tree-build3.yml` to download and verify build 3, inspect the archive, reconstruct its system partition if the payload permits independent reconstruction, and compare file hashes and paths against the checked-in tree.

Interpretation:
- If the trees match, the existing extraction is corroborated for the compared files.
- If build 3 contains only additional/newer files while the checked-in tree is otherwise the desired newer/complete snapshot, preserve the checked-in tree.
- If same-path file contents differ, do not overwrite either side automatically; investigate those differences.
- If the system transfer list leaves filesystem blocks untouched, a blank-image reconstruction is incomplete. Do not treat that comparison as valid without a compatible base image.

## Current verification boundary

No comparison report has yet been produced by the new workflow in this commit. Do not claim the existing tree is newer, identical, or fully verified until the workflow artifact is inspected. Do not claim the ROM is bootable until physical-device boot and hardware smoke tests pass.
