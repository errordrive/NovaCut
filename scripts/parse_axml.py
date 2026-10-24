#!/usr/bin/env python3
import struct
import sys
import zipfile

def parse_string_pool(data, offset):
    chunk_type, header_size, chunk_size, string_count, style_count, flags, strings_start, styles_start = struct.unpack('<HHIIIIII', data[offset:offset+28])
    is_utf8 = bool(flags & (1 << 8))
    offsets = [struct.unpack('<I', data[offset + 28 + i*4 : offset + 28 + (i+1)*4])[0] for i in range(string_count)]
    strings = []
    pool_data = data[offset + strings_start : offset + chunk_size]
    for off in offsets:
        if off >= len(pool_data):
            strings.append("")
            continue
        if is_utf8:
            idx = off
            u16len = pool_data[idx]
            idx += 1
            if u16len & 0x80:
                idx += 1
            u8len = pool_data[idx]
            idx += 1
            if u8len & 0x80:
                u8len = ((u8len & 0x7F) << 8) | pool_data[idx]
                idx += 1
            s = pool_data[idx:idx+u8len].decode('utf-8', errors='replace')
        else:
            idx = off
            if idx + 2 > len(pool_data):
                strings.append("")
                continue
            u16len = struct.unpack('<H', pool_data[idx:idx+2])[0]
            idx += 2
            if u16len & 0x8000:
                u16len = ((u16len & 0x7FFF) << 16) | struct.unpack('<H', pool_data[idx:idx+2])[0]
                idx += 2
            byte_len = u16len * 2
            s = pool_data[idx:idx+byte_len].decode('utf-16le', errors='replace')
        strings.append(s)
    return strings, offset + chunk_size

def decode_axml(data):
    offset = 0
    chunk_type, header_size, chunk_size = struct.unpack('<HHI', data[offset:offset+8])
    offset += header_size
    strings = []
    out = []
    indent = 0

    while offset < len(data):
        if offset + 8 > len(data):
            break
        chunk_type, header_size, chunk_size = struct.unpack('<HHI', data[offset:offset+8])
        if chunk_size == 0:
            break

        if chunk_type == 0x0001: # RES_STRING_POOL_TYPE
            strings, _ = parse_string_pool(data, offset)
        elif chunk_type == 0x0180: # RES_XML_RESOURCE_MAP_TYPE
            pass
        elif chunk_type == 0x0100: # RES_XML_START_NAMESPACE_TYPE
            pass
        elif chunk_type == 0x0101: # RES_XML_END_NAMESPACE_TYPE
            pass
        elif chunk_type == 0x0102: # RES_XML_START_ELEMENT_TYPE
            line_num, comment_idx, ns_idx, name_idx, attr_start, attr_size, attr_count, id_idx, class_idx, style_idx = struct.unpack('<IIIIHHHHHH', data[offset+8:offset+36])
            elem_name = strings[name_idx] if name_idx < len(strings) else f"elem_{name_idx}"
            attrs_str = []
            attr_offset = offset + 36
            for i in range(attr_count):
                if attr_offset + 20 > len(data):
                    break
                ans, aname, araw, val_size, val_res0, val_type, val_data = struct.unpack('<IIIHBBI', data[attr_offset:attr_offset+20])
                name_str = strings[aname] if aname < len(strings) else f"attr_{aname}"
                if araw != 0xFFFFFFFF and araw < len(strings) and strings[araw]:
                    val_str = strings[araw]
                elif val_type == 18: # TYPE_INT_BOOLEAN
                    val_str = "true" if val_data != 0 else "false"
                elif val_type == 3 and val_data < len(strings): # TYPE_STRING
                    val_str = strings[val_data]
                elif val_type == 1: # TYPE_REFERENCE
                    val_str = f"@0x{val_data:08x}"
                elif val_type in (16, 17): # TYPE_INT_DEC, TYPE_INT_HEX
                    val_str = str(val_data)
                else:
                    val_str = str(val_data)
                
                attrs_str.append(f'{name_str}="{val_str}"')
                attr_offset += 20
            
            attr_line = " ".join(attrs_str)
            if attr_line:
                out.append(f"{'  '*indent}<{elem_name} {attr_line}>")
            else:
                out.append(f"{'  '*indent}<{elem_name}>")
            indent += 1
        elif chunk_type == 0x0103: # RES_XML_END_ELEMENT_TYPE
            indent = max(0, indent - 1)
            line_num, comment_idx, ns_idx, name_idx = struct.unpack('<IIII', data[offset+8:offset+24])
            elem_name = strings[name_idx] if name_idx < len(strings) else f"elem_{name_idx}"
            out.append(f"{'  '*indent}</{elem_name}>")
        
        offset += chunk_size
    return "\n".join(out)

if __name__ == "__main__":
    apk_path = sys.argv[1]
    out_path = sys.argv[2]
    with zipfile.ZipFile(apk_path, 'r') as z:
        manifest_data = z.read('AndroidManifest.xml')
    decoded = decode_axml(manifest_data)
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write(decoded)
    print(f"Decoded {len(decoded.splitlines())} lines to {out_path}")
