#!/usr/bin/env python3
"""Apply Lucid Dynapunch product strings while preserving upstream GPL notices."""
from pathlib import Path
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else "theme/dynapunch/upstream")
strings = root / "app/src/main/res/values/strings.xml"
if not strings.is_file():
    raise SystemExit(f"SmartIsland strings file not found: {strings}")
text = strings.read_text(encoding="utf-8")
replacements = {
    '<string name="app_name">Smart Island</string>':
        '<string name="app_name">Lucid Dynapunch</string>',
    '<string name="app_subtitle">Floating Dynamic Overlay</string>':
        '<string name="app_subtitle">Lucid OS · Living Cutout Experience</string>',
    '<string name="master_switch_title">Smart Island</string>':
        '<string name="master_switch_title">Dynapunch</string>',
    '<string name="made_by">Made with ❤️ by Animesh Gupta</string>':
        '<string name="made_by">Lucid OS integration · SmartIsland by Animesh Gupta</string>',
}
for old, new in replacements.items():
    if old not in text:
        raise SystemExit(f"Expected upstream branding string missing: {old}")
    text = text.replace(old, new, 1)
strings.write_text(text, encoding="utf-8")
print("Applied Lucid Dynapunch branding; upstream attribution and license headers retained.")
