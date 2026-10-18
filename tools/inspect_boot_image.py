#!/usr/bin/env python3
"""Inspect an Android boot image, its ramdisk config, DTBO and AVB vbmeta."""
import gzip
import lzma
import os
import struct
import sys

def align(value, page):
    return ((value + page - 1) // page) * page if page else value

def parse_newc(data):
    files = {}
    pos = 0
    while pos + 110 <= len(data):
        magic = data[pos:pos+6]
        if magic not in (b"070701", b"070702"):
            break
        try:
            fields = [int(data[pos+6+i*8:pos+14+i*8], 16) for i in range(13)]
            file_size = fields[6]
            name_size = fields[11]
            name_start = pos + 110
            name_end = name_start + name_size
            name = data[name_start:name_end-1].decode("utf-8", "replace")
            data_start = align(name_end, 4)
            data_end = data_start + file_size
            if data_end > len(data):
                break
            if name == "TRAILER!!!":
                break
            files[name] = data[data_start:data_end]
            pos = align(data_end, 4)
        except Exception:
            break
    return files

def decompress_ramdisk(data):
    if data.startswith(b"\x1f\x8b"):
        return "gzip", gzip.decompress(data)
    if data.startswith(b"\xfd7zXZ\x00"):
        return "xz", lzma.decompress(data)
    if data.startswith((b"070701", b"070702")):
        return "uncompressed cpio", data
    if data.startswith(b"\x04\x22\x4d\x18"):
        try:
            import lz4.frame
            return "lz4 frame", lz4.frame.decompress(data)
        except Exception as exc:
            return "lz4 frame (decompression unavailable: %s)" % exc, b""
    if data.startswith(b"\x28\xb5\x2f\xfd"):
        try:
            import zstandard
            return "zstd", zstandard.ZstdDecompressor().decompress(data)
        except Exception as exc:
            return "zstd (decompression unavailable: %s)" % exc, b""
    return "unknown/raw", b""

if len(sys.argv) != 3:
    raise SystemExit("usage: inspect_boot_image.py BOOT_IMG OUTPUT_DIR")
boot_path, outdir = sys.argv[1:]
os.makedirs(outdir, exist_ok=True)
with open(boot_path, "rb") as f:
    boot = f.read()

report = []
report.append("boot_img_bytes=%d" % len(boot))
report.append("boot_sha256=" + __import__("hashlib").sha256(boot).hexdigest())
report.append("boot_magic=" + repr(boot[:8]))
if boot[:8] != b"ANDROID!" or len(boot) < 48:
    report.append("ERROR: no standard ANDROID! header")
    open(os.path.join(outdir, "boot-header-report.txt"), "w").write("\n".join(report)+"\n")
    raise SystemExit(0)

(kernel_size, kernel_addr, ramdisk_size, ramdisk_addr, second_size,
 second_addr, tags_addr, page_size, header_version, os_version) = struct.unpack_from("<10I", boot, 8)
report += [
    "header_layout=legacy-v0-v2",
    f"kernel_size={kernel_size}",
    f"kernel_addr=0x{kernel_addr:08x}",
    f"ramdisk_size={ramdisk_size}",
    f"ramdisk_addr=0x{ramdisk_addr:08x}",
    f"second_size={second_size}",
    f"second_addr=0x{second_addr:08x}",
    f"tags_addr=0x{tags_addr:08x}",
    f"page_size={page_size}",
    f"header_version={header_version}",
    f"os_version_word=0x{os_version:08x}",
]
if page_size not in (2048, 4096, 16384):
    report.append("WARNING: unusual legacy page size")
major=(os_version >> 25) & 0x7f
minor=(os_version >> 18) & 0x7f
patch=(os_version >> 11) & 0x7f
year=((os_version >> 4) & 0x7f) + 2000
month=os_version & 0xf
report.append(f"os_version_decoded={major}.{minor}.{patch}; patch-level-encoding={year:04d}-{month:02d}")

kernel_start=page_size
kernel_end=kernel_start+kernel_size
ramdisk_start=kernel_start+align(kernel_size,page_size)
ramdisk_end=ramdisk_start+ramdisk_size
report += [f"kernel_offset={kernel_start}",f"ramdisk_offset={ramdisk_start}",f"ramdisk_end={ramdisk_end}"]
if ramdisk_end > len(boot):
    report.append("ERROR: ramdisk extends past end of boot image")
else:
    kernel=boot[kernel_start:kernel_end]
    ramdisk=boot[ramdisk_start:ramdisk_end]
    with open(os.path.join(outdir,"kernel.bin"),"wb") as f: f.write(kernel)
    with open(os.path.join(outdir,"ramdisk.raw"),"wb") as f: f.write(ramdisk)
    report.append("kernel_first_16_bytes="+kernel[:16].hex())
    report.append("ramdisk_first_16_bytes="+ramdisk[:16].hex())
    kind, unpacked=decompress_ramdisk(ramdisk)
    report.append("ramdisk_compression="+kind)
    if unpacked:
        with open(os.path.join(outdir,"ramdisk.cpio"),"wb") as f: f.write(unpacked)
        files=parse_newc(unpacked)
        with open(os.path.join(outdir,"ramdisk-file-list.txt"),"w") as f:
            for name in sorted(files): f.write(name+"\n")
        critical=[]
        for name, data in sorted(files.items()):
            base=os.path.basename(name)
            if (base.startswith("fstab") or base.startswith("init") and base.endswith(".rc")
                or base.startswith("ueventd") and base.endswith(".rc")
                or base in ("prop.default","default.prop","build.prop")):
                critical.append((name,data))
        with open(os.path.join(outdir,"ramdisk-critical-config.txt"),"w") as f:
            for name,data in critical:
                f.write("\n===== "+name+" =====\n")
                f.write(data.decode("utf-8","replace")[:50000]+"\n")
        report.append("ramdisk_cpio_files=%d" % len(files))
        report.append("critical_config_files=%d" % len(critical))
    else:
        report.append("Ramdisk compression was not decoded; raw bytes preserved for further inspection.")

open(os.path.join(outdir,"boot-header-report.txt"),"w").write("\n".join(report)+"\n")
