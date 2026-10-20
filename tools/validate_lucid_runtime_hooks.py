#!/usr/bin/env python3
"""Validate cross-file contracts between LucidOS init hooks and runtime payloads.

This is static validation only; it does not prove that SELinux permits an action
or that a device mount/permission transition succeeds at runtime.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path


def validate(root: Path) -> dict:
    errors: list[str] = []
    init_path = root / "system-tree/system/etc/init/init.lucid.rc"
    contexts_path = root / "system-tree/system/etc/selinux/plat_file_contexts"
    init = init_path.read_text(encoding="utf-8", errors="replace") if init_path.is_file() else ""
    contexts = contexts_path.read_text(encoding="utf-8", errors="replace") if contexts_path.is_file() else ""
    if not init:
        errors.append("missing or empty init.lucid.rc")
    if not contexts:
        errors.append("missing or empty plat_file_contexts")

    service_paths = re.findall(r"(?m)^service\s+\S+\s+(/system/\S+)", init)
    exec_paths = re.findall(r"(?m)^\s*exec\s+[^\n]*?(/system/bin/\S+)", init)
    referenced = sorted(set(service_paths + exec_paths))
    for android_path in referenced:
        relative = android_path.removeprefix("/system/")
        local_path = root / "system-tree/system" / relative
        if not local_path.is_file():
            errors.append(f"init references missing payload: {android_path}")
        elif not (local_path.stat().st_mode & 0o111):
            errors.append(f"init references non-executable payload: {android_path}")

    desktop_path = root / "system-tree/system/bin/lucid_desktop.sh"
    desktop = desktop_path.read_text(encoding="utf-8", errors="replace") if desktop_path.is_file() else ""
    archive_match = re.search(r'^TAR_PATH="([^"]+)"', desktop, re.M)
    if not archive_match:
        errors.append("lucid_desktop.sh does not declare TAR_PATH")
    else:
        archive = archive_match.group(1).removeprefix("/system/")
        if not (root / "system-tree/system" / archive).is_file():
            errors.append(f"desktop archive missing: {archive_match.group(1)}")
    if "INIT_MARKER=" not in desktop:
        errors.append("desktop provisioning completion marker missing")
    if "start_local_adbd()" not in desktop or "-I INPUT 1 -p tcp --dport 5555 ! -i lo -j DROP" not in desktop or "ip6tables" not in desktop:
        errors.append("desktop ADB bridge lacks IPv4/IPv6 loopback-only protection")
    if "bind_once /dev" not in desktop or "bind_once /proc" not in desktop or "bind_once /sys" not in desktop:
        errors.append("desktop bind-mount idempotency contract incomplete")
    if "/usr/local/bin/lucid-sync-android-apps" not in desktop or "lucid-android-" not in desktop:
        errors.append("Android app discovery/sync integration missing")
    if "sync_termux_shortcuts()" not in desktop or "Termux:Widget" not in desktop:
        errors.append("Linux app-to-Android shortcut integration missing")
    linux_launcher = root / "system-tree/system/bin/lucid-linux-app"
    if not linux_launcher.is_file():
        errors.append("missing Android-side Linux app launcher")
    elif not (linux_launcher.stat().st_mode & 0o111):
        errors.append("Android-side Linux app launcher is not executable")

    root_toggle_path = root / "system-tree/system/bin/lucid_root_toggle.sh"
    root_toggle = root_toggle_path.read_text(encoding="utf-8", errors="replace") if root_toggle_path.is_file() else ""
    if 'SU_PATH="/system/xbin/su"' not in root_toggle:
        errors.append("root toggle target does not match /system/xbin/su")
    if "chmod 06755" not in root_toggle:
        errors.append("root-enable path lacks setuid/setgid mode 06755")
    if "mount -o remount,ro /system" not in root_toggle:
        errors.append("root toggle does not attempt to restore /system read-only")
    if "/system/xbin/su" not in contexts or "su_exec" not in contexts:
        errors.append("SELinux file contexts do not label /system/xbin/su as su_exec")

    if "setprop service.adb.tcp.port -1" not in init:
        errors.append("casting-stop action does not disable ADB TCP")
    if "setprop persist.lucid.root 0" not in init:
        errors.append("casting-stop action does not request root gate closure")

    return {
        "schema": 1,
        "scope": "static cross-file checks; no device/runtime proof",
        "referenced_init_payloads": referenced,
        "desktop_archive": archive_match.group(1) if archive_match else None,
        "error_count": len(errors),
        "errors": errors,
        "pass": not errors,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = validate(args.root.resolve())
    rendered = json.dumps(result, indent=2)
    print(rendered)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered + "\n", encoding="utf-8")
    return 0 if result["pass"] else 1


if __name__ == "__main__":
    sys.exit(main())
