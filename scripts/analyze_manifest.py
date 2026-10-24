#!/usr/bin/env python3
import xml.etree.ElementTree as ET
import re

MANIFEST_PATH = "/storage/emulated/0/ApkLab/CapCutREA/extracted/AndroidManifest.xml"

with open(MANIFEST_PATH, 'r', encoding='utf-8') as f:
    content = f.read()

# Clean up any potential xml formatting issues or extract with regex / xml parser
print("=== MANIFEST ANALYSIS ===")

# Package name & version
pkg_match = re.search(r'<manifest\s+([^>]+)>', content)
if pkg_match:
    print(f"Manifest tag: {pkg_match.group(1)}")

# SDK
sdk_match = re.search(r'<uses-sdk\s+([^>]+)>', content)
if sdk_match:
    print(f"uses-sdk: {sdk_match.group(1)}")

# Permissions
permissions = re.findall(r'<uses-permission\s+name="([^"]+)"', content)
print(f"\nDeclared Permissions ({len(permissions)}):")
for p in sorted(permissions):
    print(f"  - {p}")

# Hardware features
features = re.findall(r'<uses-feature\s+name="([^"]+)"(?:\s+required="([^"]+)")?', content)
print(f"\nHardware Features ({len(features)}):")
for f, req in features:
    print(f"  - {f} (required={req or 'true'})")

# Activities
activities = re.findall(r'<activity\s+name="([^"]+)"', content)
print(f"\nActivities ({len(activities)}):")
# find main/launcher activity
launcher_match = re.search(r'<activity\s+[^>]*name="([^"]+)"[^>]*>[\s\S]*?android\.intent\.action\.MAIN[\s\S]*?</activity>', content)
if launcher_match:
    print(f"  * Main/Launcher Activity: {launcher_match.group(1)}")
for a in activities:
    if any(k in a.lower() for k in ['edit', 'video', 'player', 'main', 'camera', 'export', 'cut', 'render']):
        print(f"  - Key editing activity: {a}")

# Services
services = re.findall(r'<service\s+name="([^"]+)"', content)
print(f"\nServices ({len(services)}):")
for s in services:
    if any(k in s.lower() for k in ['video', 'export', 'render', 'transcode', 'download', 'player']):
        print(f"  - Key service: {s}")

# Providers
providers = re.findall(r'<provider\s+name="([^"]+)"', content)
print(f"\nProviders ({len(providers)}):")
for pr in providers[:10]:
    print(f"  - {pr}")
