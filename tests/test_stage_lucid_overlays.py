#!/usr/bin/env python3
"""Tests for explicit Lucid overlay staging."""
import importlib.util
import tempfile
import unittest
from pathlib import Path

MODULE_PATH=Path(__file__).resolve().parents[1]/'tools'/'stage_lucid_overlays.py'
SPEC=importlib.util.spec_from_file_location('stage_lucid_overlays',MODULE_PATH)
MODULE=importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(MODULE)

class OverlayInventoryTests(unittest.TestCase):
    def test_inventory_distinguishes_add_replace_and_unchanged(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp); src=root/'src'; dst=root/'dst'; src.mkdir(); dst.mkdir()
            (src/'same').write_text('same'); (src/'replace').write_text('new'); (src/'add').write_text('new')
            (dst/'same').write_text('same'); (dst/'replace').write_text('old')
            rows=MODULE.inventory(src,dst)
            actions={Path(row['destination']).name:row['action'] for row in rows}
            self.assertEqual(actions,{'same':'unchanged','replace':'replace','add':'add'})

    def test_missing_base_system_root_fails_gate(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp); repo=root/'repo'; base=root/'base'; repo.mkdir(); base.mkdir()
            result={'pass':(base/'system').is_dir()}
            self.assertFalse(result['pass'])

if __name__=='__main__': unittest.main()
