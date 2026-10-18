# Native QEMU device work: Qualcomm UARTDM

This directory starts the native-QEMU path; it is not a bootable machine yet.

## Current device

`hw/char/sdm660_uartdm.c` is a SysBus MMIO device with a polling TX path,
register storage for a small subset of UARTDM, and a QEMU chardev output. It
uses the public early-boot UARTDM offsets for DMEN, SR, CR and TF. It deliberately
does **not** pretend to implement RX, IRQs, DMA/BAM, pin mux, clock/reset
controllers, the GIC, PSCI, storage, or Qualcomm secure firmware.

## Integrating into a QEMU source checkout

Copy `hw/char/sdm660_uartdm.c` into QEMU's `hw/char/` and add this line to
`hw/char/meson.build` while developing the device:

```meson
system_ss.add(files('sdm660_uartdm.c'))
```

Build a system emulator with the ARM system target enabled. The device is
registered as `sdm660-uartdm`; a board must instantiate it, attach a chardev,
map its MMIO region at a source-verified address, and route the console pinmux/
clock/reset assumptions before a guest can use it.

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

- This first C implementation has not yet been compiled against a pinned QEMU
  source revision. QEMU APIs can change, so compilation is a required next gate.
- SR reports TX-ready immediately; no baud timing or FIFO depth is simulated.
- TX output is only a debug console path, not a complete UARTDM implementation.
- Device reset does not yet model hardware reset side effects beyond register state.
- There is no native `sdm660-lavender` QEMU machine wired to this device yet.
