# Retail lavender boot-path source closure

This note records source-backed facts needed before a native QEMU board can be called `sdm660-lavender`. It deliberately excludes camera, ISP, display, audio, and other optional peripherals from the first boot target.

## Pinned source and board include chain

Kernel source repository: `xiaomi-sdm660/android_kernel_xiaomi_sdm660`

Revision: `5ce3bc43631dca83f6081757c8982c2d5f6e017c`

The retail-specific top-level include at `arch/arm/boot/dts/qcom/xiaomi/lavender.dtsi` includes:

1. `longcheer/common/longcheer-sdm660-base.dtsi`
2. `xiaomi-sdm660-common.dtsi`
3. `longcheer/lavender/lavender-base.dtsi`

This is materially better evidence than using `sdm660-mtp-lavender.dts` as if it were the retail board. The MTP DTS is explicitly labeled as a Qualcomm MTP F7A and must remain reference evidence only.

Sources:
- [retail lavender include](https://github.com/xiaomi-sdm660/android_kernel_xiaomi_sdm660/blob/5ce3bc43631dca83f6081757c8982c2d5f6e017c/arch/arm/boot/dts/qcom/xiaomi/lavender.dtsi)
- [lavender board-specific overlay](https://github.com/xiaomi-sdm660/android_kernel_xiaomi_sdm660/blob/5ce3bc43631dca83f6081757c8982c2d5f6e017c/arch/arm/boot/dts/qcom/xiaomi/longcheer/lavender/lavender-base.dtsi)
- [SoC DTSI](https://github.com/xiaomi-sdm660/android_kernel_xiaomi_sdm660/blob/5ce3bc43631dca83f6081757c8982c2d5f6e017c/arch/arm/boot/dts/qcom/sdm660.dtsi)
- [MTP reference DTS](https://github.com/xiaomi-sdm660/android_kernel_xiaomi_sdm660/blob/5ce3bc43631dca83f6081757c8982c2d5f6e017c/arch/arm/boot/dts/qcom/sdm660-mtp-lavender.dts)

## Boot-first hardware contract

| Subsystem | Source evidence | First-boot decision |
|---|---|---|
| CPU topology | SoC DTSI CPU `reg` values are `0x0..0x3` and `0x100..0x103` | Preserve MPIDR affinity layout. QEMU Cortex-A53/A57-class CPU models are only architectural approximations, not Kryo 260 reproduction. Start with one CPU, then validate PSCI and secondary-core bring-up. |
| GIC | `arm,gic-v3`, distributor `0x17a00000/0x10000`, redistributor `0x17b00000/0x100000` | Use QEMU's GICv3 implementation if compatible with the intended guest's DT and CPU interface; don't invent a Qualcomm interrupt controller. |
| Architectural timer | `arm,armv8-timer` in SoC DTSI | Use QEMU's architectural timer; verify interrupt specifiers and virtual timer behavior in the generated guest DT. |
| Boot method | SoC DTSI declares PSCI 1.0 via `smc` | A native machine must implement a compatible PSCI path for CPU_ON and reset, or deliberately boot a single CPU until PSCI exists. |
| UARTDM console candidate | `serial0 = &uartblsp1dm1`; UARTDM MMIO `0x0c170000/0x1000`, IRQ tuple `<0 108 0>` in the SoC DTSI | Keep the current polling TX device as a diagnostic milestone only. Its model maps a larger `0x2000` window and lacks IRQ support; fix its address/window and model behavior before guest use. |
| UART enablement | Base SoC DTSI marks `uartblsp1dm1` disabled. The MTP DTS enables it, but the retail include chain does not itself prove the same status is active in the final boot DTB. | **Unresolved.** Do not claim console routing is retail-correct until the final retail DTB/DTBO from the matching ROM is inspected. |
| eMMC | SoC DTSI has `sdhc_1` at `0x0c0c4000` plus command-queue range `0x0c0c5000`; aliases SDC1 as eMMC | Storage controller behavior is a later boot blocker. Do not substitute QEMU's generic SD card and call it SDM660 eMMC. |
| Clock/reset | GCC at `0x00100000` with size `0x94000`; MMCC at `0x0c8c0000` with size `0x40000` | Initially implement only the minimal clocks/resets actually touched by the boot path, based on driver/source tracing; do not fake all registers as zero. |
| Secure boot/firmware | SoC DTSI includes Qualcomm firmware/remote-processor and secure-call nodes | Likely Android/vendor blockers. Need matching firmware blobs and a documented decision on which services are stubbed versus genuinely emulated. |

## Critical distinction

The UART model currently compiles and its QTest checks are performed on QEMU's generic `virt` machine. That is useful device-model development, but it is not yet a native Snapdragon board. A board model requires QEMU machine code that owns the CPU topology, memory map, GIC, timer, reset/boot path, guest DT generation, and MMIO placement. Adding a device to `virt` is not equivalent.

## First milestone that counts as board bring-up

A meaningful next milestone is **a native QEMU machine that boots a small AArch64 Linux test kernel to a serial shell**, with a generated DTB containing verified CPU MPIDRs, memory, GICv3, architectural timer, PSCI, and the chosen UARTDM console. Android is later: the retail DTB/DTBO, boot image, eMMC layout, Qualcomm clocks/resets, and vendor secure/firmware interfaces must then be integrated incrementally.

The exact ROM is not available to CI yet. Once it is uploaded, extract and inspect its boot image, DTB/DTBO, partition metadata, and vendor firmware inventory before claiming the retail boot path is closed.
