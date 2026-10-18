# LucidOS Theme Pack (prototype)

This is a recovery-flashable add-on containing:

- The recovery pack deliberately omits the experimental framework resource overlay: its runtime mapping could not be validated against this custom-signed framework. The overlay APK remains available as a separate CI artifact for future testing.
- The Lucid Wallpapers picker, with Lucid Blue, Violet Dusk, and Cyan Horizon palette controls that request Android's native dynamic-color system.
- The app is installed as a privileged app and is allowlisted for `WRITE_SECURE_SETTINGS`. It applies Lucid Blue once on first boot, then preserves the user's palette choices; it never auto-changes the wallpaper.
- A lightweight LucidOS boot animation generated from the selected Figma wallpaper.

## Safety gate

The installer deliberately refuses to modify `/product` unless the installed product filesystem contains:

`/product/etc/lucid-base-product-verified`

with `status=clean`.

That marker must only be added after a complete product image passes filesystem checks. The currently uploaded unsigned OTA fails that check: its product transfer data reconstructs a corrupt filesystem. Do not bypass the guard or flash this pack on the current broken base.

The pack is an add-on, not a full ROM, and does not repair the product partition.
