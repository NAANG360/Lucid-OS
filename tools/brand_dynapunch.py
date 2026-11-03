#!/usr/bin/env python3
"""Apply Lucid Dynapunch product strings while preserving upstream GPL notices."""
from pathlib import Path
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else "theme/dynapunch/upstream")
string_files = [root / "app/src/main/res/values/strings.xml", root / "app/src/main/res/values-en/strings.xml"]
string_files = [path for path in string_files if path.is_file()]
if not string_files:
    raise SystemExit(f"SmartIsland strings files not found under {root}/app/src/main/res")
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
for strings in string_files:
    text = strings.read_text(encoding="utf-8")
    for old, new in replacements.items():
        if old in text:
            text = text.replace(old, new, 1)
    strings.write_text(text, encoding="utf-8")
print(f"Applied Lucid Dynapunch branding to {len(string_files)} resource sets; upstream attribution and license headers retained.")
