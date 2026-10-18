# LucidOS Theme Pack (prototype)

This is a recovery-flashable add-on containing:

- A framework resource overlay for Android's public Material tonal palettes.
- The Lucid Wallpapers picker, which sets the selected wallpaper and applies the dusk wallpaper once after the first boot.
- A lightweight LucidOS boot animation generated from the selected Figma wallpaper.

## Safety gate

The installer deliberately refuses to modify `/product` unless the installed product filesystem contains:

`/product/etc/lucid-base-product-verified`

with `status=clean`.

That marker must only be added after a complete product image passes filesystem checks. The currently uploaded unsigned OTA fails that check: its product transfer data reconstructs a corrupt filesystem. Do not bypass the guard or flash this pack on the current broken base.

The pack is an add-on, not a full ROM, and does not repair the product partition.
