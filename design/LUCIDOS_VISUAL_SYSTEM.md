# Lucid OS visual system — implementation map

## Source design file

- Figma file: [cosmosOS / LucidOS](https://www.figma.com/design/UPUZotxcIfcIdElWklOHFN/cosmosOS)
- Page: `LucidOS` (page node `0:1`)
- The file mixes LucidOS-branded screens with unrelated/older cosmosOS branding. **Lucid OS must use Lucid branding only**; the cosmos wordmark and unrelated DynaPunch device concepts are not ROM assets.

## Chosen screen families

These are the strongest directly relevant screen candidates identified from the Figma layer inventory. They are implementation references, not a claim that their iPhone frames can be copied literally onto Android.

| Priority | Figma frame | Node | Use |
| --- | --- | --- | --- |
| P0 | Control Center | `63:2` | Quick Settings composition, tile grouping, brightness/volume sliders, notifications |
| P0 | iPhone 14 - Settings | `152:2` | Settings visual hierarchy and surface/card treatment; adapt to Android Settings navigation |
| P0 | iPhone 14 - Locked | `40:243` | Lock-screen hierarchy and clock placement |
| P1 | Homescreen | `330:111` | Home-screen visual rhythm and icon/widget placement |
| P1 | delta wallpaper | `77:65` | Candidate wallpaper direction |
| P1 | wallpaper 1 | `551:34` | Candidate wallpaper |
| P1 | wallpaper 2 | `551:119` | Candidate wallpaper |
| P1 | wallpaper 3 | `551:122` | Candidate wallpaper |
| P1 | wallpaper 4 | `553:124` | Candidate wallpaper |
| P2 | Lucid Tech Logo | `2:5` | Reference only; use only if it is still the intended Lucid OS identity |

## Design rules for the Android port

1. **Keep it unmistakably Lucid.** Do not ship cosmosOS wordmarks, DynaPunch branding, iPhone frames, mock notification content, or demo values (for example, fake weather, battery percentages, meeting reminders, or demo chat messages).
2. **Translate, don't trace.** Android uses its own status bar, gesture area, Settings navigation, Quick Settings behavior, and display cutouts. Rebuild the design around Android components and the Redmi Note 7's 19.5:9-class display, not an iPhone 14 frame.
3. **One token system.** The Control Center reference visibly uses a lavender/periwinkle canvas, pale glass-like rounded cards, saturated electric-blue active controls, and dark labels. The initial Android resource palette maps those to blue primary, lavender secondary, cyan tertiary, and cool lavender-gray neutrals. Exact Figma variable/style IDs were not exposed in the retrieved frame data, so these are visual-match tokens rather than claimed copies of named Figma variables.
4. **Respect system behavior.** Tiles must invoke real Android actions; sliders must control the actual brightness/volume services; Settings rows must navigate to real destinations. Static mockups are not a functional implementation.
5. **Use overlays before replacing platform apps.** Prefer runtime resource overlays (RRO) for eligible framework/SystemUI resources. Modify Settings/SystemUI source only when the ROM has the matching source tree. Never replace privileged APKs with an unrelated prototype.
6. **Keep bootability ahead of cosmetics.** Build and validate theme artifacts against the exact framework/resource set in the release archive. Do not flash an unvalidated overlay or alter partition layout as part of a theme change.

## Current blocker discovered in the repository

The committed `system-tree/system/build.prop` declares Android SDK 35 / release 15 and LineageOS 22.2, while its build fingerprint references a Xiaomi Android 10 release. This is a mixed-generation property set, not reliable proof of the actual boot target. The repository also does not currently contain a Settings.apk or SystemUI.apk at the expected extracted paths. The release archive must be inspected to identify the real framework, Settings, SystemUI, boot image, installer and partition metadata before building an installable overlay.

## Selected visual direction

- **Primary wallpaper:** `wallpaper 4` — dusk valley, crescent moon, cyan-to-violet landscape. It is the same art used by the Homescreen frame, so the wallpaper and glassy widget treatment are coherent.
- **Alternate wallpaper:** `wallpaper 1` — blue alpine mountain with warm pink/orange highlights.
- **System accent:** saturated blue for active states, with lavender as the secondary tone and cyan reserved for tertiary highlights.
- **Surface treatment:** pale lavender-gray surfaces, rounded cards, and restrained translucent/glass effects where Android components support them. Avoid placing translucent glass effects behind small text if contrast suffers.
- **Screens not copied literally:** the Settings mockup is too sparse to replace Android's real preference hierarchy; the iPhone frame, sample notification, fake weather, meeting reminder and other demo values are excluded.

## Completion criteria

- [ ] Inspect all available LucidOS Figma screen families and extract actual color/style tokens.
- [x] Select a primary wallpaper from the source image assets (wallpaper 4) and an alpine alternate (wallpaper 1), not screenshots of device mockups.
- [ ] Confirm the release archive's actual Android framework version and target packages.
- [ ] Build a small overlay against those exact resources and fail the build on unknown target resources.
- [ ] Produce a cloud build artifact and automated validation report.
- [ ] Check that the overlay installs and can be disabled/reverted from recovery before any device test.
- [ ] Extend the same token system to Settings, SystemUI/Quick Settings, lock screen, boot animation and wallpaper only where the target ROM supports each change.
