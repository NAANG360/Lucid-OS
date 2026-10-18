# LucidOS wallpaper assets

These are the original image-fill assets imported from the connected Figma design file, not screenshots of phone mockups.

| File | Figma node | Role | Dimensions |
| --- | --- | --- | --- |
| `lucid-wallpaper-dusk.jpg` | `553:138`, inside `wallpaper 4` (`553:124`) | Preferred default: dusk valley, crescent moon, cyan/violet landscape | 564 × 1002 |
| `lucid-wallpaper-alpine.jpg` | `553:128`, inside `wallpaper 1` (`551:34`) | Alternate: dramatic blue alpine mountain with warm highlights | 564 × 1128 |

The Figma source file is linked from the [visual system map](../LUCIDOS_VISUAL_SYSTEM.md). The designs include rounded preview frames, but the files here are the source image-fill assets. Keep them as JPEGs; the `.jpg` extension reflects the actual encoded format.

These assets are committed to the repository, but the current ROM archive is a block-based OTA. Adding wallpaper files to this repository alone does not update the already-built system image. The framework overlay workflow builds and validates the color RRO separately; default-wallpaper runtime integration still needs a verified overlayable resource policy and a safe install/rollback package.
