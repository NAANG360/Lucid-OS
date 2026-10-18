# Emulator engineering roadmap

## Reality check

The SDM660 is not just an ARM64 CPU. It combines Kryo 260 CPU clusters, Adreno 512 graphics, memory controllers, GIC/interrupt routing, clocks and power domains, Qualcomm interconnects, storage, display, camera ISP, DSP, audio, Wi-Fi/Bluetooth, cellular modem and secure-world components. Android's device tree and vendor HALs expect these devices and firmware interfaces.

QEMU supports ARM system emulation, but its generic `virt` machine is not a Snapdragon 660 board. Running an Android kernel compiled for lavender on that generic machine will normally fail because its expected device tree and peripherals are absent.

## Stages

### Stage 0 — reproducible lab (implemented)
- Profile and toolchain checks.
- Generic ARM64 QEMU launcher with explicit caveats.
- CI validation.

### Stage 1 — board knowledge base (partially implemented)
- [x] Fetch a pinned public SDM660 common DTSI and a lavender board overlay in CI; preserve source revision and SHA-256 in the generated inventory.
- [x] Inventory literal register and interrupt tuples without guessing address-cell semantics; apply label-based board overlay status to the base SoC node.
- [x] Record verified boot-critical candidates and known source limitations in `emulator/snapdragon660/research/BOOT_CRITICAL_MAP.md`.
- [ ] Import the exact matching lavender DTS/DTBO set, kernel config, fstab, init scripts and proprietary-file manifests.
- Cross-reference the stock fastboot ROM's boot/vendor/product/system_ext images.
- Document physical addresses, interrupt IDs, clocks, resets, regulators, buses and dependencies from available sources.
- Identify which information is documented, inferred, or still unknown.

### Stage 2 — first SDM660-specific QEMU machine
- Fork a pinned QEMU revision in a separate experimental branch/repository.
- Add an ARMv8 machine model with the documented memory map and compatible interrupt controller/UART/timer devices.
- Add a board device tree that describes only implemented devices.
- Boot a tiny purpose-built bare-metal diagnostic first; do not start with Android.
- Add automated register-level tests for each emulated peripheral. The current Python UARTDM contract is test-only and not connected to QEMU.
- Do not claim support for a peripheral until a test exercises it.

### Stage 3 — Linux bring-up
- Add remaining devices in dependency order: interrupt controller, timers, UART, RAM, storage, clocks/reset, pinctrl, regulators, interconnect and remote processors.
- Boot a mainline kernel using a dedicated SDM660 DTS.
- Compare kernel logs against real-device logs.

### Stage 4 — Android bring-up
- Implement the storage and boot chain needed by the chosen Android kernel.
- Add the display/GPU, audio, USB, sensors and other HAL dependencies.
- Treat proprietary DSP/modem/security components as separate compatibility projects.
- Only then attempt a lavender Android userspace.

### Stage 5 — validation
- Compare behavior against a real lavender device and stock firmware.
- Test partition layout and boot image format separately from hardware emulation.
- Publish only measured compatibility, known limitations and reproducible test results.

## Important limits

A full mobile SoC is a substantial hardware-modeling project. Public kernel/device-tree source gives useful register descriptions and dependencies but does not necessarily document every hardware behavior. Proprietary firmware may not be redistributable and can be legally or technically impossible to reproduce exactly. A complete, accurate SDM660 emulator cannot honestly be promised from firmware dumps alone.
