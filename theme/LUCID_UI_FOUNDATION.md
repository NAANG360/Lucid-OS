# Lucid OS UI foundation — implementation status

## Purpose
This is the first native Android implementation pass for the Lucid visual direction documented in `design/LUCIDOS_VISUAL_SYSTEM.md`: lavender/periwinkle surfaces, white rounded cards, deep ink typography, electric-blue active controls, restrained labels, and Android-native navigation rather than literal iPhone frames.

## Added in this branch
- `control-center-app/`: native Control Center Activity with real Android settings destinations, brightness slider, media-volume slider, flashlight control, and a Quick Settings tile that opens the Control Center.
- `settings-app/`: native Lucid Settings dashboard with device and personalization categories. Rows navigate to Android's actual settings pages; Wallpapers launches the existing Lucid Wallpapers app.
- These apps are separate development packages and are not replacements for privileged platform Settings or SystemUI.

## Critical distinction
The QS tile is a real Android Quick Settings entry point, but Android's stock shade layout remains stock. Replacing the shade composition itself requires the matching SystemUI source tree/build environment or a ROM-integrated SystemUI patch. The current repository is an extracted filesystem, not a complete Android source tree, so this branch does not claim to have replaced SystemUI.

## Build and integration
The Android API 35 CI workflow is extended to compile, sign, and validate both apps as development artifacts. Their ephemeral CI signing key is not the ROM platform key. Do not sideload them as privileged/system apps or include them in a flashable ROM until their install model, permissions, target framework, and runtime behavior are verified against the actual base ZIP.

## Actual SystemUI path
The upstream LineageOS 22.2 integration touchpoints are mapped in `theme/systemui/INTEGRATION_PLAN.md`. This is a source-patch task, not something an ordinary app or a framework color overlay can complete.

## Figma reference
The exact Figma node inventory and Android adaptation rules are recorded in `design/LUCIDOS_VISUAL_SYSTEM.md`. The Figma MCP integration currently reports its Starter-plan call limit, so this implementation follows the repository's recorded node-level audit rather than pretending a fresh visual comparison was performed.
