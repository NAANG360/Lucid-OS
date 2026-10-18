# SDM660 / Redmi Note 7 public evidence ledger

Updated: 2026-10-18

This ledger separates **published facts**, **public source-code artifacts**, and **claims about dumps**. It is a research input to the emulator, not a claim that the SoC is already emulated.

## Executive finding

No reviewed public artifact establishes that a complete, verified 62.56 GB raw eMMC image for a Redmi Note 7 is available. Search results do show a technician-oriented UFI eMMC dump ZIP advertised as 126.6 MB, and a partition-table transcription for a 4/64 GB lavender unit. These are useful forensic leads, not proof of a complete eMMC image or a complete SoC firmware set. A service dump may contain selected partitions only. Treat every image as untrusted input; verify hash, file format, partition map, build fingerprint and provenance before use.

## Hardware target

Qualcomm's SDM660 brief describes an 8-core Kryo 260 CPU split into four high-performance cores (up to 2.2 GHz) and four low-power cores (up to 1.8 GHz), Adreno 512 GPU, Hexagon 680 DSP with HVX, Spectra 160 dual ISP, LPDDR4/4x, eMMC 5.1, and (for SDM660) X12 LTE modem. The Redmi Note 7 platform is not just a CPU: its boot chain, interrupt controller, timers, clocks/resets, power management, storage, display, touch, USB, audio, camera, WLAN/Bluetooth, modem, secure-world interfaces and vendor firmware all matter.

Official source: https://www.qualcomm.com/processors/application-processors/products/sdm660
Product brief PDF: https://www.qualcomm.com/content/dam/qcomm-martech/dm-assets/documents/sdm-sda660-product_brief_87-pu779-1.pdf

## Public source-code leads

| Artifact | What it contributes | What it does not prove |
|---|---|---|
| https://github.com/xiaomi-sdm660/android_device_xiaomi_sdm660-common | Common Android configuration for Xiaomi SDM660 devices; lists lavender support and proprietary-file manifests | Does not implement a QEMU machine or disclose all silicon behavior |
| https://github.com/SDM660-Project/device_xiaomi_lavender | Lavender-specific device tree for a 4.19 kernel era; device specs and proprietary-file manifest | Android device tree is not a hardware emulator |
| https://github.com/ParsimoniousLavender/device_xiaomi_sdm660-common | Alternative common device-tree history and manifests | Fork differences need commit-by-commit comparison |
| https://github.com/SpiceOS-devices/android_device_xiaomi_sdm660_common_4.19 | Another common tree for SDM660 devices | Does not itself provide full Qualcomm BSP |
| https://github.com/CEKIKOFGAMERS/7c2ae174020fdb25ef33d2c01656fcf1 | Public lavender partition table transcription, including boot-critical and security-related partitions | A text table is not a raw disk dump and may vary by device/storage variant |
| https://androidfilehost.com/?fid=10620683726822074016 | Advertised “Redmi Note 7_lavender [UFI_EMMC Dump]” ZIP, listed as 126.6 MB | File size strongly suggests a selected/service dump, not the whole 58.24 GiB user area |
| https://www.serviceemmc.com/2020/01/log-info-emmcemcp-redmi-note-7-lavender.html | eMMC identity/log details: Samsung DH6DAB, eMMC 5.1, ~62.56 GB user area, SDM660 / lavender build metadata | Device log only; not a full dump |

## Dump collection plan

For each public dump or firmware package, record:
- exact source URL and retrieval date;
- published size, downloaded size and cryptographic SHA-256;
- whether it is a full raw user area, GPT/partition image, selected partition set, fastboot ROM, OTA, or firehose/service package;
- device variant, storage capacity, region, build fingerprint and security patch;
- partition names, start/end LBAs, image sizes, sparse/raw state and hash per partition;
- boot-chain artifacts (PBL is mask ROM and generally not dumped as ordinary storage; then available XBL/ABL, RPM, TZ, hyp, devcfg, keymaster, modem, DSP, bluetooth, WLAN and related images);
- licensing and redistribution constraints. Do not commit proprietary firmware blobs or device-unique secrets to the repository without authorization.

Do not run unknown firehose programmers, flashing scripts, or binaries on a physical device. Do not use another person's unique modem/NV/QCN/security data. For research, prefer hashes, headers, partition metadata, symbol maps and de-identified logs.

## Emulation implications

Generic QEMU `virt` is useful for ARM64 Linux testing but does not model SDM660. A credible emulator needs a custom QEMU machine/platform description plus device models and a matching boot path. Implement in layers:

1. Document memory map, GIC, timer, UART, PSCI/SMP and DT handoff using public kernel DTS/source.
2. Build a custom machine model or a carefully scoped out-of-tree QEMU board prototype; don't label a generic `virt` VM as SDM660.
3. Add storage controller/boot media and validate Linux serial boot.
4. Add Qualcomm-specific clocks, resets, interconnect, pinctrl, PMIC, RPM/SCM and secure monitor interfaces needed by the selected kernel.
5. Add display/touch and Android-required devices, then GPU/DSP/modem only if feasible and supported by documentation.
6. Keep unsupported peripherals explicit and observable; stubs must fail visibly rather than pretend to work.

The biggest unknown is not CPU instruction emulation; it is the proprietary boot chain and the many tightly coupled Qualcomm peripherals/firmware. This is a research-and-development project, not a quick config change.
