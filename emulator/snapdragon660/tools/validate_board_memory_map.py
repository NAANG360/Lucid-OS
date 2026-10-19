#!/usr/bin/env python3
"""Validate source-derived SDM660 address candidates and their overlap status."""
import json
from pathlib import Path

MAP = Path(__file__).resolve().parents[1] / "board-memory-map.json"

def validate(data):
    assert data["schema_version"] == 1
    assert data["target"].endswith("SDM660")
    entries = data["entries"]
    names = [entry["name"] for entry in entries]
    assert len(names) == len(set(names)), "duplicate map entry names"
    ranges = []
    for entry in entries:
        base, size = int(entry["base"], 0), int(entry["size"], 0)
        assert base >= 0 and size > 0, f"invalid range: {entry['name']}"
        assert entry["evidence"] in {"confirmed-dtsi-declaration", "inferred"}
        assert entry["implementation"] in {
            "not-implemented", "prototype-device-not-board-mapped",
            "prototype-board-mapped-unverified", "implemented-and-tested"
        }
        ranges.append((base, base + size, entry["name"]))
    ranges.sort()
    for previous, current in zip(ranges, ranges[1:]):
        assert previous[1] <= current[0], (
            f"overlapping MMIO candidates: {previous[2]} and {current[2]}"
        )
    # Guard against accidentally presenting a research map as an implemented board.
    assert not any(e["implementation"] == "implemented-and-tested" for e in entries), (
        "Do not mark board devices implemented until native-board runtime tests exist"
    )
    return len(entries)

if __name__ == "__main__":
    data = json.loads(MAP.read_text(encoding="utf-8"))
    count = validate(data)
    print(f"validated {count} source-derived SDM660 address candidates; none claimed board-implemented")
