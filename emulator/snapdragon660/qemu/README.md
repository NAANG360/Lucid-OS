# Native QEMU device work: Qualcomm UARTDM

This directory now contains the first native `sdm660-lavender` QEMU machine implementation. It is a bring-up prototype, not yet a validated guest-boot platform and not an Android-capable emulator.

## Current device

`hw/char/sdm660_uartdm.c` is a SysBus MMIO device with polling TX and RX paths,
a bounded 256-byte receive FIFO, a QEMU chardev, and a small subset of UARTDM
register behavior. Offsets are checked against the pinned lavender kernel's
`msm_serial_hs_hwreg.h` by CI. This remains a bring-up stub: it does **not**
implement interrupts, DMA/BAM, baud timing, pin mux, clock/reset controllers,
the GIC, PSCI, storage, or Qualcomm secure firmware.

## Native machine prototype

`hw/arm/sdm660_lavender.c` registers a distinct `sdm660-lavender` machine
without inheriting QEMU's generic `virt` board. It starts implementing the
lavender board contract:

- 4 GiB RAM at the current 0x80000000 boot-memory candidate;
- CPU MPIDR affinity values 0x0–0x3 and 0x100–0x103, with QEMU Cortex-A53/A72
  architectural models as explicit Kryo 260 approximations;
- GICv3 distributor/redistributor at the source-declared SDM660 addresses;
- architectural timer wiring and a generated minimal guest DTB;
- the existing UARTDM device mapped at 0x0c170000; UARTDM interrupt support is deliberately not advertised yet because the device model still lacks an IRQ output.

These are initial code paths, **not yet compile/runtime-verified**. The CPU
models do not reproduce Kryo 260 microarchitecture; the UART's retail console
enablement still needs confirmation from the exact lavender DTB/DTBO; PSCI,
Qualcomm clocks/resets, eMMC, secure firmware, and Android boot remain
unimplemented. The generated DTB is a bring-up scaffold, not a replacement for
the retail device tree.

The CI workflow patches pinned QEMU v10.0.0 to compile the board as a separate
machine and checks that `sdm660-lavender` appears in QEMU's machine registry.
A machine-registration check alone is not evidence that a kernel or Android
boots.

## Integrating into a QEMU source checkout

Copy `hw/char/sdm660_uartdm.c` into QEMU's `hw/char/` and add this line to
`hw/char/meson.build` while developing the device:

```meson
system_ss.add(files('sdm660_uartdm.c'))
```

Build a system emulator with the ARM system target enabled. The device is registered as `sdm660-uartdm`. The CI harness temporarily instantiates it on QEMU's generic `virt` platform bus solely to test the device model; that is not a Snapdragon board. The board model must map its MMIO region at a source-verified SDM660 address and route the console pinmux/clock/reset assumptions before a guest can use it.

Do not add the device to the default `virt` board and label that a Snapdragon
machine. The native board still needs the actual SDM660 CPU topology, memory
layout, GICv3 and architectural timer, PSCI/secondary-core startup, clock/reset
and power-controller behavior, storage, and an Android-compatible boot chain.

## Address provenance

The public pinned source used by the inventory workflow is:

- `xiaomi-sdm660/android_kernel_xiaomi_sdm660`
- revision `5ce3bc43631dca83f6081757c8982c2d5f6e017c`
- `arch/arm/boot/dts/qcom/sdm660.dtsi`

The SoC DTSI defines the UART through label `uartblsp1dm1`; the device-tree
inventory workflow also reads a separate MTP lavender overlay because the base
SoC DTSI and retail-device configuration do not express identical enablement.
A machine implementation must consume the retail lavender DT/overlay combination,
not blindly copy the MTP status or assume every SoC node is enabled.

## Known limitations before compile/runtime validation

- The CI workflow compiles against pinned QEMU v10.0.0 and runs a QTest-driven runtime test on the generic `virt` platform bus. It checks TX-ready status, DMEN mode handling, CR readback, TX enable/disable gating, TX reset behavior, and chardev output. This validates narrow peripheral behavior only; it does not validate a Snapdragon board, Qualcomm CPU topology, or guest boot.
- SR reports TX-ready immediately; no baud timing or FIFO depth is simulated.
- TX output is only a debug console path, not a complete UARTDM implementation.
- Device reset does not yet model hardware reset side effects beyond register state.
- The UARTDM MMIO window is 0x1000 bytes, matching the pinned SoC DTSI declaration. There is no native `sdm660-lavender` QEMU machine wired to this device yet.
