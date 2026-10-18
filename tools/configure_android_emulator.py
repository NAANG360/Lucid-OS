#!/usr/bin/env python3
"""Set the disposable GitHub Actions AVD display profile to phone-like dimensions."""
from pathlib import Path

config = Path.home() / ".android/avd/test.avd/config.ini"
if not config.is_file():
    raise SystemExit(f"AVD config not found: {config}")

settings = {
    "hw.lcd.width": "1080",
    "hw.lcd.height": "2340",
    "hw.lcd.density": "440",
    "hw.ramSize": "3072",
    "hw.cpu.ncore": "4",
}
lines = [
    line for line in config.read_text().splitlines()
    if not any(line.startswith(key + "=") for key in settings)
]
lines.extend(f"{key}={value}" for key, value in settings.items())
config.write_text("\n".join(lines) + "\n")
print("Configured 1080x2340 / 440 dpi AVD profile; this is not Xiaomi hardware emulation.")
