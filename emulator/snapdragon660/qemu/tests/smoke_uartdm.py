#!/usr/bin/env python3
"""Runtime test for the QEMU UARTDM model: MMIO status and TX chardev output."""
from __future__ import annotations

import json
import os
import re
import socket
import subprocess
import sys
import tempfile
import time
from pathlib import Path


def connect_unix(path: str, proc: subprocess.Popen[bytes], timeout: float = 20.0) -> socket.socket:
    deadline = time.monotonic() + timeout
    last_error: OSError | None = None
    while time.monotonic() < deadline:
        if proc.poll() is not None:
            raise RuntimeError(f"QEMU exited early with status {proc.returncode}")
        sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        try:
            sock.connect(path)
            sock.settimeout(3.0)
            return sock
        except OSError as exc:
            last_error = exc
            sock.close()
            time.sleep(0.1)
    raise TimeoutError(f"QEMU socket did not become ready: {path}: {last_error}")


def recv_line(sock: socket.socket) -> str:
    data = bytearray()
    while not data.endswith(b"\n"):
        chunk = sock.recv(1)
        if not chunk:
            raise RuntimeError("QEMU socket closed before a complete response")
        data.extend(chunk)
    return data.decode("utf-8", errors="replace").strip()


def qmp_command(sock: socket.socket, command: str, arguments: dict | None = None) -> dict:
    payload = {"execute": command}
    if arguments is not None:
        payload["arguments"] = arguments
    sock.sendall((json.dumps(payload) + "\r\n").encode())
    while True:
        response = json.loads(recv_line(sock))
        if "event" in response:
            continue
        if "error" in response:
            raise RuntimeError(f"QMP {command} failed: {response['error']}")
        if "return" in response:
            return response["return"]


def qtest_command(sock: socket.socket, command: str) -> str:
    sock.sendall((command + "\n").encode())
    response = recv_line(sock)
    if not response.startswith("OK"):
        raise RuntimeError(f"QTest command {command!r} failed: {response}")
    return response


def main() -> int:
    qemu = os.environ.get("QEMU_BIN", "qemu-src/build/qemu-system-aarch64")
    with tempfile.TemporaryDirectory(prefix="sdm660-uartdm-test-") as temp:
        qmp_path = str(Path(temp) / "qmp.sock")
        qtest_path = str(Path(temp) / "qtest.sock")
        tx_path = str(Path(temp) / "uart-tx.bin")
        log_path = str(Path(temp) / "qemu.log")
        args = [
            qemu, "-machine", "virt", "-accel", "tcg", "-nodefaults",
            "-display", "none", "-monitor", "none", "-serial", "none", "-S",
            "-qmp", f"unix:{qmp_path},server=on,wait=off",
            "-qtest", f"unix:{qtest_path},server=on,wait=off",
            "-chardev", f"file,id=uart,path={tx_path}",
            "-device", "sdm660-uartdm,chardev=uart",
        ]
        with open(log_path, "wb") as log:
            proc = subprocess.Popen(args, stdout=log, stderr=subprocess.STDOUT)
            qmp = None
            qtest = None
            try:
                qmp = connect_unix(qmp_path, proc)
                greeting = json.loads(recv_line(qmp))
                if "QMP" not in greeting:
                    raise RuntimeError(f"Unexpected QMP greeting: {greeting}")
                qmp_command(qmp, "qmp_capabilities")
                tree = qmp_command(qmp, "human-monitor-command",
                                   {"command-line": "info mtree"})
                if not isinstance(tree, str):
                    raise RuntimeError("QMP did not return the QEMU memory tree")
                # The platform bus maps the device's region as a subregion, while
                # SysBusDevice's own mmio.addr may remain unset. Inspect the guest
                # physical memory tree rather than relying on "info qtree".
                match = re.search(
                    r"(?m)^\\s*([0-9a-fA-F]{8,16})-([0-9a-fA-F]{8,16})"
                    r"[^\\n]*: sdm660-uartdm\\s*$",
                    tree,
                )
                if not match:
                    raise RuntimeError("Could not find UARTDM in QEMU's guest physical memory map:\\n" + tree)
                base = int(match.group(1), 16)
                end_addr = int(match.group(2), 16)
                if end_addr - base + 1 != 0x2000:
                    raise RuntimeError(f"Unexpected UARTDM MMIO range: 0x{base:x}-0x{end_addr:x}")
                print(f"UARTDM MMIO base discovered from QEMU device tree: 0x{base:x}")

                qtest = connect_unix(qtest_path, proc)
                status = qtest_command(qtest, f"readl 0x{base + 0x0a4:x}")
                status_value = int(status.split()[-1], 0)
                if status_value & 0x4 == 0:
                    raise RuntimeError(f"UARTDM TX-ready bit was clear: {status}")
                print(f"PASS: UARTDM status register reports TX-ready (0x{status_value:08x})")

                qtest_command(qtest, f"writel 0x{base + 0x0a8:x} 0x4")
                qtest_command(qtest, f"writel 0x{base + 0x100:x} 0x41")
                print("Wrote 'A' to UARTDM TX FIFO after enabling TX")
                qmp_command(qmp, "quit")
                proc.wait(timeout=10)
                payload = Path(tx_path).read_bytes() if Path(tx_path).exists() else b""
                if b"A" not in payload:
                    raise RuntimeError(f"UARTDM TX byte was not delivered to chardev; captured {payload!r}")
                print(f"PASS: UARTDM transmitted byte 0x41 to chardev ({payload!r})")
                print("PASS: UARTDM runtime MMIO and TX path")
                return 0
            finally:
                if qtest is not None:
                    qtest.close()
                if qmp is not None:
                    qmp.close()
                if proc.poll() is None:
                    proc.terminate()
                    try:
                        proc.wait(timeout=5)
                    except subprocess.TimeoutExpired:
                        proc.kill()
                        proc.wait(timeout=5)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        sys.exit(1)
