# SDM660 bring-up evidence: boot-critical candidates

**Purpose:** turn the public Linux device-tree source into an actionable implementation queue without pretending the current prototype is a real Snapdragon emulator.

## Pinned source

- Repository: `xiaomi-sdm660/android_kernel_xiaomi_sdm660`
- Revision: `5ce3bc43631dca83f6081757c8982c2d5f6e017c`
- File: `arch/arm/boot/dts/qcom/sdm660.dtsi`
- Canonical source: https://github.com/xiaomi-sdm660/android_kernel_xiaomi_sdm660/blob/5ce3bc43631dca83f6081757c8982c2d5f6e017c/arch/arm/boot/dts/qcom/sdm660.dtsi

The CI workflow downloads that exact revision plus a pinned lavender MTP overlay from okta-10/mystic_kernel_sdm660-4.4 revision 3fbc81d0cd7ee4116a72926b3777ea0379be9dd7, then runs the inventory parser. The resulting JSON preserves raw register and interrupt cell tuples, includes each source's SHA-256, and applies the overlay's UART status override.

## Confirmed DTS declarations

These are literal declarations in the pinned common SoC DTSI, **not proof that a QEMU model exists or that the final retail lavender device tree uses every node unchanged**. The overlay source is an MTP configuration and is not a substitute for the exact final DTB/DTBO from the user's matching ROM.

| Block | DTS node / compatible | Literal register tuple(s) | Status / notes |
|---|---|---|---|
| Interrupt controller | `interrupt-controller@17a00000`, `arm,gic-v3` | `<0x17a00000 0x10000>`, `<0x17b00000 0x100000>` | GIC distributor and redistributor ranges as commented by source |
| BLSP UART candidate | `serial@0c170000`, `qcom,msm-uartdm-v1.4` | `<0xc170000 0x1000>` | Marked `disabled` in this common DTSI; board-level DTS must be inspected before selecting console |
| Second BLSP UART candidate | `serial@0c1b0000`, `qcom,msm-uartdm-v1.4` | `<0xc1b0000 0x1000>` | Also marked `disabled` in this common DTSI |
| eMMC controller candidate | `sdhci@c0c4000`, `qcom,sdhci-msm-v5` | `<0xc0c4000 0x1000>`, `<0xc0c5000 0x1000>` | Source aliases SDC1 as the eMMC slot; second range is named `cmdq_mem` |
| SD-card controller candidate | `sdhci@c084000`, `qcom,sdhci-msm-v5` | `<0xc084000 0x1000>` | Source aliases SDC2 as SD card |
| GCC clock/reset controller | `clock-controller@100000`, `qcom,gcc-sdm660` | `<0x100000 0x94000>` | Exposes clock and reset cells; register semantics still need implementation research |
| Multimedia clock controller | `clock-controller@c8c0000`, `qcom,mmcc-sdm660` | `<0xc8c0000 0x40000>` | Multimedia clocks |
| GPU clock controller | `clock-controller@5065000`, `qcom,gpu-sdm660` | `<0x5065000 0x10000>` | Clock/reset block only; this is not an Adreno 512 GPU model |

Interrupt properties should remain raw tuples until interpreted with the actual interrupt parent and binding. `reg` tuples also require each bus's `#address-cells`, `#size-cells`, and `ranges` context. Do not copy addresses blindly into a QEMU machine from this table.

## Source discrepancy that must not be papered over

The pinned MTP overlay also enables UFS nodes, whereas public Redmi Note 7 retail specifications describe eMMC 5.1. That mismatch is a warning that this MTP overlay may not match the exact retail storage configuration. Treat eMMC/UFS bring-up as unresolved until the final DTB/DTBO and partition table from the actual target ROM are inspected; do not implement storage from the MTP overlay alone.

## Implementation order

1. **Board-specific source closure:** collect the exact lavender DTB/DTS, all included SoC and PMIC DTSIs, DTBO overlays, and boot image metadata from the user's matching ROM or public source. Resolve which UART is enabled and the actual RAM/reserved-memory map.
2. **Native QEMU board skeleton:** add a machine type only once the supported address map, CPU topology, memory layout, interrupt controller, reset/boot flow, and console path are defined. Do not make a misleading alias of `virt` and label it Snapdragon.
3. **Early boot path:** implement or reuse the correct GICv3 and architectural timer; add a UART device compatible with the *actual* enabled UART (the Qualcomm UARTDM is not PL011); pass a generated board DTB to a Linux test kernel.
4. **Storage:** model the Qualcomm SDHCI controller and its required command/interrupt behavior; attach a raw eMMC image only after partition data and firmware are available.
5. **Android/vendor services:** RPM/RPMh-like resource control, GCC/MMCC clocks and resets, SPMI PMIC, SCM/TrustZone interfaces, remote processors, and device-specific vendor services are later dependencies.
6. **Acceleration:** Adreno 512, Hexagon 680, Spectra 160 and modem behavior are separate major subsystems, not features implied by CPU boot.

## Known blocker

The Python model at `emulator/snapdragon660/model/peripherals.py` is a deterministic UARTDM TX contract for unit tests, not a CPU emulator and not connected to QEMU's guest MMIO bus. A separate native QEMU SysBus UARTDM prototype now has polling TX/RX paths, a bounded receive FIFO, and a chardev. It still has no IRQ, DMA/BAM, baud timing, clock/reset, pinmux, GIC, or board integration. The QEMU workflow's compilation and device-realization checks are not evidence of a native SDM660 machine or Android boot.

UARTDM register offsets are cross-checked in CI against the pinned kernel header `drivers/tty/serial/msm_serial_hs_hwreg.h` at the same revision. The modeled offsets include DMEN `0x03c`, SR `0x0a4`, CR `0x0a8`, CSR `0x0a0`, IMR `0x0b0`, TF `0x100`, and RF `0x140`. These offsets describe only the register subset currently modeled; they do not imply full hardware compatibility.
