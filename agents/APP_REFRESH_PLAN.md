# Lucid app refresh — prepared, not built

This branch stages the next ROM build. **Do not run the ROM assembly workflow until the maintainer explicitly asks to build.**

## What the next ROM build will rebuild from source

| Component | Package / output | Version in prepared source |
|---|---|---|
| Lucid Setup | `com.cosmos.setup` / `LucidSetup.apk` | `0.3.1-dev` |
| Lucid System Updater | `com.lucid.os.updater` / `LucidSystemUpdater.apk` | `0.4.5-dev`, versionCode 8 |
| Lucid Wallpapers | `com.lucid.os.wallpapers` / `LucidWallpapers.apk` | `0.2.1-dev` |
| Lucid About | `com.lucid.os.about` / `LucidAbout.apk` | `0.2.1-dev` |
| Lucid Launcher | `tech.lucid.launcher` / `LucidLauncher.apk` | `0.1.2-dev`, versionCode 3 |
| Boot animation | `LucidOS-bootanimation.zip` | regenerated from current Lucid artwork |

The setup, updater, and wallpaper sources were synchronized from the current main branch. The launcher source was brought over from the DynaPunch launcher development branch and added to the ROM assembly workflow.

## Integration changes

- The assembly workflow now watches launcher, setup, wallpaper, About, asset, and updater source paths so edits to those components can trigger a future build on the designated build branch.
- The workflow builds the launcher alongside the other Lucid apps and places it in `product/priv-app/LucidLauncher`.
- The integration step checks that all five source-built Lucid APKs exist in the final partition tree and writes SHA-256 reports for them.
- The updater version has been bumped so the next APK is not mistaken for the previously installed version.
- OTA metadata must not be changed to a future version until the actual ROM artifact has been built, validated, uploaded, and its exact checksum and size are known.

## Important boundaries

- **No build has been run for this branch.** APK compilation, final ROM packaging, and install/boot validation remain pending.
- Android framework and vendor apps should stay matched to the selected Android 15 base. Do not replace random platform APKs with files from a different Android release; that can break signatures, shared libraries, permissions, or framework compatibility.
- The launcher is packaged as an additional selectable HOME app. Making it the default launcher is a separate change and must be verified on-device.
- A successful build is not proof that the update was installed. After the maintainer authorizes a build, validate the ROM ZIP and OTA metadata, then verify the install path and app versions on the phone.
