# SDM660 / lavender implementation evidence

This file records the upstream sources actually inspected for the Snapdragon 660 target. It is a hardware-model research note, not a claim that QEMU already emulates this SoC.

## Primary public sources

- Device tree, branch `4.19-rebase`: https://github.com/SDM660-Project/device_xiaomi_lavender
- Shared SDM660 Android platform tree, branch `11`: https://github.com/xiaomi-sdm660/android_device_xiaomi_sdm660-common
- QEMU ARM system emulation docs: https://www.qemu.org/docs/master/system/target-arm
- Qualcomm SDM660 product brief: https://www.qualcomm.com/media/documents/files/sdm-sda660-product-brief.pdf

## Verified lavender facts from the device tree

The upstream lavender `README.md` identifies the platform as Qualcomm SDM660 / Snapdragon 660, Kryo 260 (8 cores), Adreno 512, LPDDR4X, and eMMC 5.1. It specifies a 2340x1080 display and shipped Android 9. The device tree's `BoardConfig.mk`:
- inherits `device/xiaomi/sdm660-common/BoardConfigCommon.mk`;
- selects `vendor/mystic-lavender_defconfig`;
- uses a separate DTBO image;
- sets system and vendor image sizes and system-as-root;
- points recovery to `rootdir/etc/fstab.qcom`.

The device's `device.mk` inherits the shared SDM660 product configuration and installs Qualcomm-specific audio, camera shim, IR, FM, powerhint, USB and Wi-Fi configuration. These are strong indicators that an Android guest requires substantially more than a CPU and a generic virtual board.

## What these sources do NOT provide

Android device trees and product makefiles describe the guest's expected platform and configuration; they are not a complete electrical/RTL specification of the SoC. They do not, by themselves, give a lawful, complete model of every proprietary Qualcomm block or a complete eMMC image. Do not infer register addresses, interrupt lines, reset sequencing, secure-world behavior, or firmware contents from names alone.

## Correct implementation boundary

A genuine emulator target must be a QEMU machine/device implementation, not merely a JSON profile or a renamed `virt` machine. The implementation needs at least:
1. A QEMU machine type registered as `sdm660-lavender` with an explicit memory map and board initialization.
2. CPU model semantics: QEMU can execute AArch64 instructions, but generic Cortex CPU emulation is not equivalent to reproducing Kryo 260 microarchitecture, clocks, or performance.
3. Real modeled boot-critical peripherals and documented register behavior, beginning with interrupt controller, timers, UART/console, memory, storage, and reset/clock dependencies.
4. A matching Linux kernel and device tree, then staged Android bring-up.
5. Separate future work for Qualcomm secure world / SCM, RPM, SPMI PMIC, clocks/resets, eMMC, display, Adreno 512, Hexagon DSP, camera ISP, Wi-Fi/Bluetooth and modem.

## Current status

The current `emulator/snapdragon660` files are an explicitly non-bootable prototype scaffold. The DTS deliberately disables its placeholder memory node; the generic QEMU ARM `virt` platform is not a Snapdragon emulation and is not acceptable as a completion criterion. CI success currently validates the scaffold and host QEMU capability inventory only. The next meaningful milestone is a native QEMU C machine/device model, not another generic ARM Linux boot.
