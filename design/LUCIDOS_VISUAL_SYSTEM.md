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
3. **One token system.** The Control Center reference visibly uses a lavender/periwinkle canvas, pale glass-like rounded cards, saturated electric-blue active controls, and dark labels. The initial Android resource palette is generated with Material Color Utilities from a blue source color: primary blue, same-hue lower-chroma secondary, a +60° tertiary hue, and low-chroma neutral palettes. Lavender/periwinkle remains the visual surface direction; wallpaper cyan is artwork rather than a falsely labeled system accent. Exact Figma variable/style IDs were not exposed in the retrieved frame data, so these are visual-match tokens rather than claimed copies of named Figma variables.
4. **Respect system behavior.** Tiles must invoke real Android actions; sliders must control the actual brightness/volume services; Settings rows must navigate to real destinations. Static mockups are not a functional implementation.
5. **Use overlays before replacing platform apps.** Prefer runtime resource overlays (RRO) for eligible framework/SystemUI resources. Modify Settings/SystemUI source only when the ROM has the matching source tree. Never replace privileged APKs with an unrelated prototype.
6. **Keep bootability ahead of cosmetics.** Build and validate theme artifacts against the exact framework/resource set in the release archive. Do not flash an unvalidated overlay or alter partition layout as part of a theme change.

## Implementation and validation status

- **Built in cloud CI:** Lucid Wallpapers app, Lucid Blue / Violet Dusk / Cyan Horizon palette controls, a Material tonal resource-overlay prototype, and a matching boot animation based on wallpaper 4. The static overlay is retained as a test artifact only and is not included in the ROM while runtime mapping remains unverified.
- **Native system theming path:** the palette controls write Android's `theme_customization_overlay_packages` secure setting, using a Lucid seed color and `TONAL_SPOT` style. The app is intended to ship in `/product/priv-app` with an explicit `WRITE_SECURE_SETTINGS` allowlist; it does not replace the wallpaper automatically at boot.
- **Not yet proven:** whether the custom framework resource overlay can be mapped by the exact ROM's overlay manager. The available host AAPT2 did not provide a conclusive overlayability report, so the static overlay must remain a prototype until runtime mapping is verified.
- **Confirmed install blocker:** the unsigned OTA's product transfer list writes only 123,902 blocks, while the product filesystem spans 601,750 blocks. Reconstructing it from an empty super image produces invalid ext4 extents and a corrupt root directory. The current ZIP must not be treated as a bootable full install. See `docs/ROM_INSTALLER_FINDINGS.md` and `docs/PRODUCT_REBUILD_INPUT.md`.
- **Required source input:** a complete extracted `product_mod` tree is needed to rebuild a full ext4 product image, apply Android file contexts, add the Lucid assets, validate with `e2fsck`, and replace the partial OTA payload with a full-block payload.

The Control Center and Settings Figma frames remain design targets for later SystemUI/Settings-specific work. This repository currently has extracted system files and APK resources, not the matching Android source tree; a real Quick Settings layout rewrite cannot be honestly claimed from a wallpaper or color overlay alone.

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
