#!/usr/bin/env python3
import os
import zipfile
import hashlib

ARCHIVE_PATH = "/storage/emulated/0/ApkLab/CapCutREA/CapCut_19.8.0.apks"
EXTRACT_DIR = "/storage/emulated/0/ApkLab/CapCutREA/extracted"

os.makedirs(EXTRACT_DIR, exist_ok=True)

print(f"Extracting {ARCHIVE_PATH} to {EXTRACT_DIR}...")
with zipfile.ZipFile(ARCHIVE_PATH, 'r') as z:
    for member in z.namelist():
        target_path = os.path.join(EXTRACT_DIR, member)
        if not os.path.exists(target_path):
            z.extract(member, EXTRACT_DIR)
            print(f"Extracted: {member}")
        else:
            print(f"Already exists: {member}")

print("\nVerifying extracted files:")
for item in sorted(os.listdir(EXTRACT_DIR)):
    item_path = os.path.join(EXTRACT_DIR, item)
    if os.path.isfile(item_path):
        size = os.path.getsize(item_path)
        sha256 = hashlib.sha256()
        with open(item_path, 'rb') as f:
            while chunk := f.read(65536):
                sha256.update(chunk)
        print(f"{item:30} {size:12} bytes | SHA256: {sha256.hexdigest()}")
