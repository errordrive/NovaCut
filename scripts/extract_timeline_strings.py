#!/usr/bin/env python3
import zipfile
import re

APK_PATH = "/storage/emulated/0/ApkLab/CapCutREA/extracted/base.apk"

def extract_strings_from_dex(dex_bytes):
    # Quick string extraction from DEX
    # Dex header: string_ids_size at 0x38 (4 bytes), string_ids_off at 0x3C (4 bytes)
    import struct
    str_count, str_off = struct.unpack('<II', dex_bytes[0x38:0x40])
    strings = []
    for i in range(min(str_count, 50000)):
        off_pos = str_off + i * 4
        if off_pos + 4 > len(dex_bytes): break
        s_off = struct.unpack('<I', dex_bytes[off_pos:off_pos+4])[0]
        # read uleb128 length
        idx = s_off
        length = 0
        shift = 0
        while idx < len(dex_bytes):
            b = dex_bytes[idx]
            idx += 1
            length |= (b & 0x7f) << shift
            if (b & 0x80) == 0:
                break
            shift += 7
        end_idx = dex_bytes.find(b'\x00', idx)
        if end_idx != -1 and end_idx - idx < 500:
            try:
                s = dex_bytes[idx:end_idx].decode('utf-8')
                strings.append(s)
            except:
                pass
    return strings

with zipfile.ZipFile(APK_PATH, 'r') as z:
    for dex_name in ['classes28.dex', 'classes22.dex']:
        data = z.read(dex_name)
        strs = extract_strings_from_dex(data)
        matches = [s for s in strs if any(k in s for k in ['timerange', 'track', 'canvas', 'draft', 'keyframe', 'transition', 'segment', 'materials', 'video_track', 'audio_track', 'text_track'])]
        print(f"--- Matches in {dex_name} ({len(matches)}) ---")
        for m in sorted(list(set(matches)))[:40]:
            print("  ", m)
