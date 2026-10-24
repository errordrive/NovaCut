#!/usr/bin/env python3
import zipfile
import re

APK_PATH = "/storage/emulated/0/ApkLab/CapCutREA/extracted/base.apk"

with zipfile.ZipFile(APK_PATH, 'r') as z:
    for dex_name in [f'classes{i}.dex' for i in ['', 2, 10, 13, 22, 28, 31]]:
        name = dex_name if dex_name != 'classes.dex' else 'classes.dex'
        try:
            data = z.read(name)
        except KeyError:
            continue
        
        # Look for draft JSON keys
        for key in [b'"canvas_config"', b'"materials"', b'"tracks"', b'"segments"', b'"extra_info"', b'"video_track"', b'"audio_track"']:
            idx = 0
            while True:
                pos = data.find(key, idx)
                if pos == -1: break
                idx = pos + len(key)
                snippet = data[max(0, pos-40):min(len(data), pos+120)]
                # print printable part
                clean = "".join(chr(b) if 32 <= b < 127 else " " for b in snippet)
                print(f"[{name}] {clean.strip()}")
                if idx > pos + 500: break
