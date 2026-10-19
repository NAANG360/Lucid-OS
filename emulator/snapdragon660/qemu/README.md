# Native QEMU machine: Qualcomm SDM660 / Xiaomi lavender

This directory contains the first native `sdm660-lavender` QEMU machine
prototype. It does not inherit QEMU's generic `virt` machine. It is still a
bring-up prototype, not a validated Linux platform and not an Android-capable
emulator.

## Native machine implementation

`hw/arm/sdm660_lavender.c` currently attempts to provide:

- a 4 GiB RAM region at the current 0x80000000 boot-memory candidate;
- eight CPU slots with source-derived MPIDR affinities `0x0..0x3` and
  `0x100..0x103`;
- QEMU Cortex-A53/A72 CPU models as architectural approximations of Kryo 260
  Silver/Gold cores (not microarchitectural or performance equivalence);
- GICv3 distributor/redistributor placement at source-declared SDM660 addresses;
- architectural timer wiring and a generated minimal guest DTB;
- UARTDM mapped at 0x0c170000 using the separate `sdm660-uartdm` device.

These paths are being compiled and smoke-tested in CI. They are not yet proof
that a guest kernel boots. The generated DTB is an initial bring-up scaffold,
not a substitute for the retail lavender DTB/DTBO.

## UARTDM device status

`hw/char/sdm660_uartdm.c` implements a small polling TX/RX subset, a bounded
256-byte RX FIFO, a QEMU chardev, and an initial TX-ready interrupt latch routed
to the GIC. CI checks register offsets against the pinned lavender kernel header
and validates the ISR offset against the pinned BLSP UART driver's register map.
The interrupt model is not yet validated against the full Linux UART driver.
DMA/BAM, RX interrupt semantics, baud timing, pin mux, and clock/reset
dependencies remain missing. The DT UART interrupt specifier is source-derived
but still needs guest-level validation.

## Important unresolved boot blockers

- Retail lavender UART enablement and final console routing must be verified
  from the exact retail DTB/DTBO, not inferred from the MTP overlay.
- PSCI and secondary-core startup are not implemented.
- GCC/MMCC clocks, resets, power sequencing, eMMC, and Qualcomm secure/firmware
  services are not implemented.
- CPU models are not Kryo 260 implementations.
- The matching Android ROM has not yet been uploaded to CI, so its boot image,
  DTB/DTBO, partition metadata, and vendor firmware have not been inspected.

## CI validation

The workflow patches pinned QEMU v10.0.0, compiles the ARM system emulator,
checks that `sdm660-lavender` is registered, starts it paused for a smoke test,
and separately runs UARTDM device tests on the generic `virt` platform.
The UARTDM QTests validate that peripheral only; they are not evidence of a
Snapdragon board boot. The native-machine smoke test validates initialization
only; it does not claim Linux or Android boot.

## Address provenance

Primary public source:

- `xiaomi-sdm660/android_kernel_xiaomi_sdm660`
- revision `5ce3bc43631dca83f6081757c8982c2d5f6e017c`
- `arch/arm/boot/dts/qcom/sdm660.dtsi`

The retail board include chain begins at
`arch/arm/boot/dts/qcom/xiaomi/lavender.dtsi` and includes
`longcheer/common/longcheer-sdm660-base.dtsi`,
`xiaomi-sdm660-common.dtsi`, and
`longcheer/lavender/lavender-base.dtsi`. The MTP lavender DTS is reference
evidence only; it must not be treated as the retail board configuration.
