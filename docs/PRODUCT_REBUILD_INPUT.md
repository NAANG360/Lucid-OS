# Required input to rebuild the full product partition

The uploaded `LucidOS-unsigned.zip` is not self-contained for `product`. Its transfer list writes only 123,902 blocks, while the product filesystem spans 601,750 blocks. Reconstructing that package from a blank super image produces a corrupt product filesystem. The main installer discards the old super storage, so the missing product blocks cannot safely be assumed to remain.

The rebuild workflow now first tries to recover the product filesystem from the original LineageOS ZIP. It will use that route only if the original transfer list touches the entire declared filesystem and the reconstructed ext4 image passes `e2fsck -fn`. If either check fails, the workflow stops and requires the complete product tree described below.

The local file `product.img` listed at about 507 MB is the LUCID OTA block payload reconstructed from its transfer list; it is **not** a complete product filesystem image. Do not upload that file as the source.

## One-time Termux upload

Upload the complete extracted `product_mod` directory that contains the product filesystem's root folders (for example `app`, `etc`, `media`, `overlay`, `priv-app`, `framework`, `lib`, and `lib64`).

Run:

```bash
pkg install zstd -y
cd /sdcard/lucidos/tobelucid || exit 1
tar -I 'zstd -T0 -6' -cf /sdcard/lucidos/product-mod-tree.tar.zst product_mod
unset GITHUB_TOKEN GH_TOKEN
gh release upload tobelucid-source-v1 /sdcard/lucidos/product-mod-tree.tar.zst --repo NAANG360/Lucid-OS
```

This preserves file contents, symlinks, and mode bits while compressing the tree for upload. The cloud workflow then rebuilds a full ext4 product image with Android file contexts, installs the Lucid palette overlay, wallpaper app, and boot animation into it, validates the filesystem, and replaces the partial product OTA payload with a full-block payload.

After the asset upload succeeds, open **Actions → Rebuild Full LucidOS Product Partition → Run workflow**. The workflow will stop rather than produce a flashable ZIP if the tree is incomplete or the rebuilt filesystem fails validation.
