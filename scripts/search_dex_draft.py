#!/usr/bin/env python3
import zipfile
import re

APK_PATH = "/storage/emulated/0/ApkLab/CapCutREA/extracted/base.apk"

print("Searching for draft / project schema strings in DEX files...")
keywords = [
    b"draft_content.json",
    b"draft_meta_info.json",
    b"canvas_config",
    b"draft_materials",
    b"main_track",
    b"source_timerange",
    b"target_timerange"
]

with zipfile.ZipFile(APK_PATH, 'r') as z:
    for name in z.namelist():
        if name.endswith('.dex'):
            data = z.read(name)
            found = [kw.decode('ascii') for kw in keywords if kw in data]
            if found:
                print(f"In {name}: found {found}")
