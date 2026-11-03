# Lucid Dynapunch

Lucid Dynapunch is Lucid OS's branded integration of the upstream SmartIsland Android project.

## Upstream and licensing

- Upstream: https://github.com/agupta07505/SmartIsland
- Pinned upstream commit: `149efd1fde40416b93116ee7c958c2793a5d4459`
- Upstream license: GNU GPL-3.0; retain upstream copyright and license notices.
- Source is tracked as a Git submodule at `theme/dynapunch/upstream`. Clone with `git clone --recurse-submodules` or run `git submodule update --init --recursive`.

This is an integration, not a claim that the upstream code was authored by Lucid. Local branding changes are applied by `tools/brand_dynapunch.py` during CI without removing attribution or license notices.

## Product intent

- Present the experience as **Lucid Dynapunch**, using Lucid's dark/violet visual identity.
- Keep the floating island physically aligned with the real display cutout and status-bar edge; do not render a detached pill below the notch.
- Preserve SmartIsland's event handling, notification/media behavior, gestures, configuration, privacy controls and accessibility support.
- Calibrate layout for Redmi Note 7 (lavender, waterdrop notch) while retaining responsive layouts for other displays.
- Keep permissions explicit and user-controlled. Android overlay, notification-listener, accessibility, usage and battery-exemption privileges are special access; they cannot legitimately be silently pre-granted to an ordinary APK. The ROM may grant supported permissions through its privileged integration only after reviewing the platform security model.

## Build

```bash
git submodule update --init --recursive
python3 tools/brand_dynapunch.py theme/dynapunch/upstream
cd theme/dynapunch/upstream
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The `Lucid Dynapunch CI` workflow builds, tests, installs on an Android 15 emulator, captures the app's setup screen, and uploads the APK and evidence. Overlay behavior still requires a separate permission-enabled emulator/device test; a setup-screen screenshot alone is not proof that the island overlays the cutout correctly.
