<div align="center">

# LUCID OS

**A lavender ROM project with its own look, its own tools, and a long way to go.**

[![Device](https://img.shields.io/badge/device-Redmi_Note_7%20%2F%207S-7c6ce7?style=for-the-badge)](https://github.com/NAANG360/Lucid-OS)
[![Android](https://img.shields.io/badge/Android-15-3ddc84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/NAANG360/Lucid-OS)
[![Status](https://img.shields.io/badge/status-in_development-f0b45a?style=for-the-badge)](https://github.com/NAANG360/Lucid-OS/releases)

*Made for the Xiaomi Redmi Note 7 / Note 7S — codename `lavender`.*

</div>

---

## The idea

Lucid OS is starting with the **Xiaomi Redmi Note 7 / Note 7S (`lavender`)** as its first device. Right now, the work is focused on getting the ROM, updater, and core experience right for this phone.

But the ambition is bigger than one device. The long-term plan is to grow Lucid beyond a single-device ROM into **its own line of phones** — with hardware designed around the software, a consistent identity, and a unified experience. That's a future goal, not a claim that Lucid-branded phones are available today; the current project is still being built and tested on lavender.

The idea is a daily-use system that feels intentional: a distinct visual identity, useful built-in tools, and a desktop environment that can grow beyond the phone screen.

This is an active work in progress, not a claim that every feature is finished or every build is safe to flash. Builds and components are shared as they become available; check the notes for the exact state of each release.

## What's here

- **System Updater** — an updater app with public OTA metadata and a recovery installer package.
- **Lucid setup and system apps** — the project’s setup flow, about screen, wallpapers, and boot animation.
- **Desktop experiments** — Linux/chroot assets and supporting scripts for the larger-screen experience.
- **Build tooling** — GitHub Actions workflows for packaging, validation, and release publishing.

Some components are further along than others. A downloadable ZIP is not, by itself, proof of a successful install or first boot.

## Downloads

Head to **[Releases](https://github.com/NAANG360/Lucid-OS/releases)** for available builds and tools.

| You need | Where to look |
|---|---|
| ROM package | Latest release notes and ROM ZIP asset |
| Updater APK | Latest release assets |
| Recovery updater installer | Asset ending in `lavender-recovery.zip` |
| OTA metadata | [`ota/stable.json`](ota/stable.json) |

**Before flashing:** confirm the package is for `lavender`, read the release notes, keep a backup, and make sure you have a recovery path. An unsigned ROM may require a compatible installation method. Flashing can erase data or leave the device unbootable.

## Device & project status

- **Target:** Xiaomi Redmi Note 7 / Redmi Note 7S (`lavender`)
- **Base direction:** Android 15
- **Status:** in development
- **Physical-device verification:** varies by artifact; use each release's notes rather than assuming the ROM, updater, and recovery installer have all been tested together.

The updater’s recovery ZIP is an installer for the updater app; it is **not** the ROM itself. The OTA manifest should point to the ROM archive and include that archive’s matching SHA-256 and byte size.

## For contributors

Start with [`AGENTS.md`](AGENTS.md) for the short project rules, then check [`agents/`](agents/) for build notes and task-specific guidance. Human-facing documentation lives here; the agent notes are for repository work and should not be treated as release marketing.

When changing a build or release workflow, verify the actual artifact, checksum, device target, and public download URL. Keep release notes plain: say what changed, what was tested, and what still needs testing. No victory laps for a build that has only compiled.

## License & source

This repository contains project files and tooling for Lucid OS. Check individual directories and files for their applicable licenses and attribution before redistributing bundled components. Base-ROM components may have their own licensing terms.

---

<div align="center">

**Built for lavender. Still being built.**

</div>
