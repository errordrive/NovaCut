#!/usr/bin/env python3
import zipfile
import re
import xml.etree.ElementTree as ET

BASE_APK = "/storage/emulated/0/ApkLab/CapCutREA/extracted/base.apk"
EN_APK = "/storage/emulated/0/ApkLab/CapCutREA/extracted/split_config.en.apk"

print("=== CAPCUT 19.8.0 UI/UX STRUCTURAL ANALYSIS ===")

with zipfile.ZipFile(BASE_APK, 'r') as z:
    names = z.namelist()
    layouts = [n for n in names if n.startswith('res/layout') and n.endswith('.xml')]
    print(f"Total layout XML files in base.apk: {len(layouts)}")
    
    # Filter key editor layouts
    editor_layouts = [n for n in layouts if any(k in n.lower() for k in [
        'editor', 'timeline', 'preview', 'track', 'playhead', 'toolbar', 
        'bottom_bar', 'trim', 'split', 'speed', 'filter', 'canvas', 'cut'
    ])]
    print(f"Key editor-related layouts ({len(editor_layouts)}):")
    for l in sorted(editor_layouts)[:30]:
        print(f"  - {l}")

print("\nSearching for toolbar action and menu identifiers...")
with zipfile.ZipFile(EN_APK, 'r') as z:
    for n in z.namelist():
        if 'strings' in n:
            data = z.read(n)
            # Find common CapCut English UI strings
            sample_terms = [
                b"Edit", b"Audio", b"Text", b"Stickers", b"Effects", 
                b"Filters", b"Ratio", b"Adjust", b"Background",
                b"Split", b"Speed", b"Animation", b"Style", b"Delete",
                b"Volume", b"Transform", b"Cutout", b"Mask", b"Overlay",
                b"Replace", b"Opacity", b"Reverse", b"Freeze", b"Voice effects"
            ]
            found = [t.decode('utf-8') for t in sample_terms if t in data]
            print(f"In {n}: verified user-facing menu terms: {found[:15]}")
