#!/usr/bin/env python3
import os
import zipfile
import re

EXTRACT_DIR = "/storage/emulated/0/ApkLab/CapCutREA/extracted"

apks = [f for f in os.listdir(EXTRACT_DIR) if f.endswith('.apk')]

print(f"=== APK INVENTORY ({len(apks)} APK files) ===")
for apk in sorted(apks):
    apk_path = os.path.join(EXTRACT_DIR, apk)
    size_mb = os.path.getsize(apk_path) / (1024 * 1024)
    print(f"\n--- {apk} ({size_mb:.2f} MB) ---")
    with zipfile.ZipFile(apk_path, 'r') as z:
        names = z.namelist()
        dex_files = [n for n in names if re.match(r'^classes\d*\.dex$', n)]
        so_files = [n for n in names if n.endswith('.so')]
        assets = [n for n in names if n.startswith('assets/')]
        res_files = [n for n in names if n.startswith('res/')]
        print(f"  Total files: {len(names)}")
        print(f"  DEX files: {len(dex_files)} ({', '.join(dex_files)})")
        print(f"  Native libraries (.so): {len(so_files)}")
        if so_files:
            print("  Top native libraries:")
            for so in sorted(so_files)[:20]:
                info = z.getinfo(so)
                print(f"    - {so} ({info.file_size / 1024:.1f} KB)")
            if len(so_files) > 20:
                print(f"    ... and {len(so_files) - 20} more")
        print(f"  Assets: {len(assets)}")
        if assets:
            asset_dirs = set(a.split('/')[1] for a in assets if '/' in a and len(a.split('/')) > 1)
            print(f"  Asset categories/subdirs: {sorted(list(asset_dirs))[:15]}")
        print(f"  Res files: {len(res_files)}")
