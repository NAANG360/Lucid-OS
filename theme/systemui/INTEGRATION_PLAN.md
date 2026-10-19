# Lucid SystemUI / Quick Settings integration plan

## Target source line

The ROM project is declared as LineageOS 22.2 / Android 15. The corresponding upstream SystemUI source reference inspected for this plan is `LineageOS/android_frameworks_base`, branch `lineage-22.2`. This is a reference tree, not a claim that the current ROM archive was built from this exact revision.

## Verified source touchpoints

- `packages/SystemUI/res/layout/qs_panel.xml` — root `QSContainerImpl`, expanded `QSPanel`, scroll container, header include, footer actions, and customization include.
- `packages/SystemUI/res/layout/quick_status_bar_expanded_header.xml` — `QuickStatusBarHeader` and `QuickQSPanel` used in the collapsed/partially expanded shade.
- `packages/SystemUI/src/com/android/systemui/qs/QSContainerImpl.java` — panel container, clipping, expansion and layout behavior.
- `packages/SystemUI/src/com/android/systemui/qs/QuickStatusBarHeader.java` — header layout and quick tile strip.
- `packages/SystemUI/src/com/android/systemui/qs/QSPanelController.java` — controller for the actual QS panel, tile lifecycle, brightness and interaction wiring.
- `packages/SystemUI/src/com/android/systemui/qs/QuickQSPanelController.java` and `PagedTileLayout.java` — collapsed tile strip and full tile-page behavior.

## Planned Lucid translation

1. Keep Android's real `QSHost`, tile services, controllers, accessibility, shade expansion and notification interaction. Do not build a fake notification shade in an ordinary app.
2. Rework the panel/header layouts and eligible SystemUI drawable/color/dimension resources to match the Figma-derived Lucid language: periwinkle/lavender base, pale rounded groups, electric-blue active states, dark ink text, compact labels, and restrained shadows.
3. Retain the real brightness controller/slider and tile callbacks. Volume controls may be placed in the expanded surface only if they are backed by Android's actual audio service and have a deliberate interaction model.
4. Use a source patch for layout and behavior changes. Use RRO only for resources explicitly marked overlayable and verified against the exact target APK; do not assume an overlay can replace arbitrary layouts or privileged behavior.
5. Validate compile, install, shade expansion/collapse, tile customization, brightness updates, notification visibility, gesture navigation, display cutout/insets, accessibility and rotation on a build for `lavender` before packaging.

## Current blocker / no false claim

The LUCID-OS repository currently contains an extracted filesystem and build scripts, not the full Android framework/SystemUI source tree. The original ROM ZIP expected from the user is an install package, not necessarily a source tree. Therefore the app-level Control Center is implemented as a working source candidate, and a real SystemUI replacement is **not yet implemented**. The native Quick Settings tile only provides an entry point to that app; it does not replace the stock shade.

## Upstream reference files

- [QS panel layout](https://github.com/LineageOS/android_frameworks_base/blob/lineage-22.2/packages/SystemUI/res/layout/qs_panel.xml)
- [Expanded header layout](https://github.com/LineageOS/android_frameworks_base/blob/lineage-22.2/packages/SystemUI/res/layout/quick_status_bar_expanded_header.xml)
- [QS panel controller](https://github.com/LineageOS/android_frameworks_base/blob/lineage-22.2/packages/SystemUI/src/com/android/systemui/qs/QSPanelController.java)
- [QS container implementation](https://github.com/LineageOS/android_frameworks_base/blob/lineage-22.2/packages/SystemUI/src/com/android/systemui/qs/QSContainerImpl.java)
