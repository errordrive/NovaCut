#!/usr/bin/env python3
import zipfile
import re

APK_PATH = "/storage/emulated/0/ApkLab/CapCutREA/extracted/split_config.arm64_v8a.apk"

with zipfile.ZipFile(APK_PATH, 'r') as z:
    so_infos = [z.getinfo(n) for n in z.namelist() if n.endswith('.so')]

so_infos.sort(key=lambda x: x.file_size, reverse=True)

print(f"Total native libraries: {len(so_infos)}")
print(f"{'Library Name':45} {'Size (KB)':12} {'Compressed (KB)':15}")
print("-" * 75)
for s in so_infos:
    lib_name = s.filename.split('/')[-1]
    print(f"{lib_name:45} {s.file_size/1024:10.1f} KB {s.compress_size/1024:12.1f} KB")
