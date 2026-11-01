# Lucid Complete Redesign Handoff

This branch is the consolidated starting point for the next Lucid OS redesign pass.

## Branch ancestry and inherited work

Created from `feature/lucid-root-provider` at the time of branch creation. That branch is a direct descendant of `fix/gemini-lucid-integration-regressions`, which itself descends from `feature/lucid-all-apps-refreshed-connect`.

This carries forward:
- Gemini's consolidated app refresh and integration work.
- Follow-up Gemini integration/regression fixes.
- Lucid Root Provider work and its daemon/client, init and SELinux policy files.
- Dynamic Island work, app build scripts, emulator QA workflow, ROM integration workflow and OTA changes present in the parent branch.

Do not cherry-pick those ancestors again; they are already in this branch's history.

## Design authority

- Mobile Lucid OS: the existing `cosmosOS` Figma file, especially the `LucidOS` page and its Assets page. Treat the actual frames as the source of truth; do not reinterpret Lucid as a generic dark-card dashboard.
- Desktop: the existing `Lucid Desktop` Figma file.
- Figma is reference-only for this implementation. Do not edit, reorganize, rename, publish or overwrite Figma files.
- Preserve supplied artwork and assets. In particular, retain the Bubbles wallpaper and all existing user-provided wallpapers. Never silently replace missing artwork with generated or stock substitutes.
- Keep Lucid's visual language intentional: atmospheric layered surfaces, considered translucency, rounded geometry, expressive gradients, violet/lavender/periwinkle/blue/pink accents where the relevant reference calls for them, precise typography, iconography and motion. Do not force every screen into one palette; follow each screen's reference.

## Required redesign scope

Audit and then redesign/fix all existing Lucid surfaces, preserving working behavior:
1. Launcher and home/workspace.
2. Settings and appearance provider.
3. Control Centre app.
4. Wallpapers and wallpaper selection/application.
5. Updater and OTA flow.
6. Lucid Desktop and its root-access flow.
7. Lucid About.
8. Dynamic Island.
9. Shared motion, icons, assets, manifests, permissions, package integration and build/release scripts.

No app is to be omitted, especially Lucid About.

## Critical Quick Settings / SystemUI requirement

The standalone Control Centre app and its Quick Settings tile are not the native Android Quick Settings shade. The RRO prototype is not a completed SystemUI redesign.

The deliverable must include a real Android 15 / LineageOS 22.2 SystemUI Quick Settings and notification-shade implementation:
- Native shade expansion/collapse and transitions.
- Actual QS tiles and platform tile state/actions (not simulated toggles).
- Native brightness controller and slider.
- Notification stack, grouping, ranking, privacy, heads-up, dismissal and inline actions preserved.
- Header, quick-tile strip, full tile grid/pages, footer/customization and settings entry points.
- Keyguard, biometric, power menu, emergency, screenshot and screen-recording flows preserved.
- Insets/cutout, gesture navigation, portrait/landscape, accessibility, TalkBack, large text, RTL and reduced-motion behavior tested.
- Retain Android's real SystemUI controllers, services, QSHost and tile lifecycle. Do not implement a fake shade in an ordinary app.

Use `theme/systemui/INTEGRATION_PLAN.md` and `theme/systemui/LUCID_SYSTEMUI_ROADMAP.md` as starting documentation, but verify them against the exact Android source revision and target framework. The repository's app-level build scripts and extracted filesystem are not a substitute for a matching Android source checkout. If the exact source/build environment is unavailable, report the native SystemUI task as blocked; do not claim the app prototype or an RRO palette overlay completes it.

## Engineering and QA gates

- Inspect existing code and branch history before replacing anything; preserve Gemini's useful behavior and fix regressions rather than blindly rewriting.
- Establish one coherent shared token/component system from the actual Figma references, while allowing screen-specific palettes and layouts.
- Build all app modules, including About and Dynamic Island.
- Build native SystemUI against the matching Android 15 / LineageOS 22.2 source and correct device/framework configuration.
- Exercise real interactions and permissions, not just Activity launch.
- Run emulator/device tests for crash, ANR, blank/unresponsive screens, navigation, state persistence, rotation, insets and accessibility.
- Capture screenshots of every app and key SystemUI states. Visually inspect the screenshots, fix clipping, spacing, contrast, typography, layout and state bugs, then capture the final screenshots again.
- Publish build logs, test reports, screenshots and artifacts. Clearly label development-signed APKs versus platform-signed ROM packages.
- Do not call a ROM flashable or safe until the complete integrated ROM has been built and boot-tested on the target device. Keep the Redmi Note 7 (lavender) target explicit.

## Important current limitations to re-check

- The native SystemUI implementation is not complete merely because the app-level Control Centre and a tile service exist.
- The root-provider SELinux/authorization integration and Desktop root gating require device-level validation.
- A successful APK install/launch test is not proof that an app is responsive or functionally correct.
- Any ROM image/partition reconstruction and base-archive issues must be independently verified before release.

## Working rule

Make changes in this branch only. Keep the existing Figma sources untouched. Prefer complete, tested implementation over cosmetic placeholders. Report exactly what built, what was exercised, what screenshots were inspected, and what remains blocked.
