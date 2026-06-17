package io.github.tt432.eyelib.importer.addon;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** .brarchive 二进制存档格式解码器。
 * 格式：8B magic + 4B entryCount(LE) + 4B version + entryCount×256B entry records + 合并JSON。
 * @author TT432 */
final class BrArchiveDecoder {

    private BrArchiveDecoder() {
    }

    record Entry(String name, int offsetInContent) {
    }

    static byte[] extractJson(Path file) throws IOException {
        byte[] data = Files.readAllBytes(file);
        if (data.length < 16) {
            throw new IOException("brarchive file too short");
        }
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        long magic = buf.getLong();
        if (magic != 0x267052A0B125277DL) {
            throw new IOException("Invalid brarchive magic");
        }
        int entryCount = buf.getInt();
        buf.getInt();
        int contentStart = 16 + entryCount * 256;
        if (contentStart >= data.length) {
            return new byte[0];
        }
        int contentLen = data.length - contentStart;
        byte[] json = new byte[contentLen];
        System.arraycopy(data, contentStart, json, 0, contentLen);
        return json;
    }

    static List<Entry> parseEntries(Path file) throws IOException {
        byte[] data = Files.readAllBytes(file);
        if (data.length < 16) {
            throw new IOException("brarchive file too short");
        }
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        long magic = buf.getLong();
        if (magic != 0x267052A0B125277DL) {
            throw new IOException("Invalid brarchive magic");
        }
        int entryCount = buf.getInt();
        buf.getInt();
        byte[] raw = data;
        var entries = new ArrayList<Entry>(entryCount);
        int contentStart = 16 + entryCount * 256;
        for (int i = 0; i < entryCount; i++) {
            int recordOff = 16 + i * 256;
            int nameLen = raw[recordOff] & 0xFF;
            String name = nameLen > 0 ? new String(raw, recordOff + 1, Math.min(nameLen, raw.length - recordOff - 1), StandardCharsets.UTF_8) : "";
            entries.add(new Entry(name, contentStart));
        }
        return entries;
    }
}
