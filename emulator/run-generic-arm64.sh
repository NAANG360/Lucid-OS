#!/usr/bin/env bash
set -euo pipefail
if [[ $# -lt 2 || $# -gt 3 ]]; then
  echo "Usage: $0 <ARM64-kernel-Image> <initramfs.cpio.gz> [serial-log.txt]" >&2
  exit 2
fi
KERNEL="$(realpath "$1")"
INITRD="$(realpath "$2")"
LOG="${3:-qemu-arm64-serial.log}"
for f in "$KERNEL" "$INITRD"; do
  [[ -f "$f" ]] || { echo "Missing input: $f" >&2; exit 2; }
done
QEMU="${QEMU_SYSTEM_AARCH64:-qemu-system-aarch64}"
command -v "$QEMU" >/dev/null || {
  echo "qemu-system-aarch64 not found. Install qemu-system-arm." >&2
  exit 127
}
exec "$QEMU" \
  -machine virt,gic-version=3 \
  -cpu cortex-a57 \
  -smp 8 -m 4096 \
  -kernel "$KERNEL" -initrd "$INITRD" \
  -append "console=ttyAMA0 earlycon=pl011,0x9000000 loglevel=8 panic=-1" \
  -nographic -no-reboot \
  2>&1 | tee "$LOG"
