# Snapdragon Lab — free lavender emulation workbench

**Status: experimental emulator foundation, not yet a Snapdragon 660 hardware emulator.**

This project builds a reproducible, free QEMU environment for ARM64 boot-flow experiments and ROM forensics, alongside a machine-readable hardware reference for the Redmi Note 7 (lavender). It intentionally does not claim that QEMU's generic `virt` board reproduces Qualcomm's SDM660.

## What it can do now

- Describe the target lavender profile (8 ARM64 cores, 4 GiB RAM target, 1080×2340 display profile).
- Validate QEMU availability and required command-line features.
- Launch an ARM64 guest on QEMU's generic `virt` board when a compatible generic ARM64 kernel and initramfs are supplied.
- Generate a hardware/firmware inventory from source references and the ROM package validator already in this repository.
- Run reproducible headless boot tests in free GitHub Actions.

## What it cannot do yet

- Boot the stock Redmi Note 7 kernel as though a real SDM660 board were present.
- Emulate Adreno 512, Spectra ISP, Hexagon DSP, Qualcomm modem, PMIC, secure world, or proprietary firmware.
- Guarantee LucidOS boots on physical lavender. That requires fixing the package and testing on a device or a sufficiently accurate board model.

QEMU's generic ARM `virt` board exposes generic devices, not the Snapdragon 660 peripheral map. CPU instruction-set compatibility alone is not enough to emulate a phone SoC. See the design notes in `docs/EMULATOR_ROADMAP.md`.

## Quick start (Linux x86_64 or ARM64 host)

Install QEMU system emulation and Python 3. On Debian/Ubuntu:

```sh
sudo apt-get install qemu-system-arm qemu-utils
```

Inspect host support:

```sh
python3 emulator/tools/check_qemu.py
```

Boot a **generic** ARM64 Linux kernel and initramfs:

```sh
./emulator/run-generic-arm64.sh /path/to/Image /path/to/initramfs.cpio.gz
```

This is a reference guest on QEMU `virt`, not the lavender Android ROM. It uses a Cortex-A57 CPU model as an available ARMv8 reference; it does not reproduce Kryo 260 microarchitecture.

## Target profile

The default profile is recorded in `emulator/config/lavender-profile.json`. The CPU model is explicitly marked as an approximation, not a Snapdragon CPU implementation. Resolution is metadata for future UI/emulator integration; it does not force a guest display mode on its own.

## CI

The `SDM660 Emulator Foundation` workflow runs static checks and validates the QEMU launcher on free GitHub-hosted runners. CI success means the launcher and test harness work; it does **not** mean a complete SDM660 emulator exists.

## Licensing

Scripts and original documentation in this directory are MIT licensed. Third-party firmware, proprietary blobs, kernel sources and ROMs retain their own licenses and are not redistributed here.
