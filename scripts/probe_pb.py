import sys

path = sys.argv[1] if len(sys.argv) > 1 else r'data\t2-render-protobuf-client.bin'
data = open(path, 'rb').read()
print('total size:', len(data))

pos = 0
fields = {}
while pos < len(data):
    tag = data[pos]; pos += 1
    field_num = tag >> 3
    wire_type = tag & 7
    if wire_type == 2:  # length-delimited
        length = 0; shift = 0
        while True:
            b = data[pos]; pos += 1
            length |= (b & 0x7F) << shift
            shift += 7
            if not (b & 0x80): break
        fields.setdefault(field_num, []).append(length)
        pos += length
    elif wire_type == 5:
        pos += 4
    elif wire_type == 1:
        pos += 8
    elif wire_type == 0:
        while data[pos] & 0x80: pos += 1
        pos += 1
    else:
        print('unexpected wire_type', wire_type, 'field', field_num, 'at pos', pos - 1)
        break

names = {1: 'METADATA', 2: 'THREADS', 3: 'CLASS_SOURCES', 4: 'METHOD_SOURCES',
         5: 'LINE_SOURCES', 6: 'TIME_WINDOWS', 7: 'TIME_WINDOW_STATISTICS', 8: 'CHANNEL_INFO'}
for fn in sorted(fields.keys()):
    nm = names.get(fn, '?')
    sizes = fields[fn]
    print('field %d (%s): %d occurrences, sizes=%s' % (fn, nm, len(sizes), sizes[:6]))
