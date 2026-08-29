"""Decode .brarchive: 8B magic + 4B entryCount(LE) + 4B version + entryCount*256B records + merged JSON."""
import struct, sys, zipfile, io, json

pack = sys.argv[1]
member = sys.argv[2]

with zipfile.ZipFile(pack) as z:
    data = z.read(member)

magic, count, version = struct.unpack_from('<QII', data, 0)
assert magic == 0x267052A0B125277D, hex(magic)
print(f"entries={count} version={version}", file=sys.stderr)
names = []
off = 16
for i in range(count):
    rec = data[off:off+256]
    off += 256
    name = rec.split(b'\0')[0].decode('utf-8', 'replace')
    names.append(name)
payload = data[off:].decode('utf-8', 'replace')
print(f"names={names}", file=sys.stderr)
print(payload)
