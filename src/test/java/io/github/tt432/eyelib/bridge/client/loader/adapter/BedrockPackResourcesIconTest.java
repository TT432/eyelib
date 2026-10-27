package io.github.tt432.eyelib.bridge.client.loader.adapter;

import net.minecraft.server.packs.resources.IoSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 包图标桥接的落盘契约：图标流每次可重取、无图标回落 null、探测后不残留打开的 zip 句柄。
 */
class BedrockPackResourcesIconTest {
    private static final byte[] ICON = {(byte) 0x89, 'P', 'N', 'G'};

    @Test
    void iconStreamIsReopenableAndDoesNotKeepZipHandle(@TempDir Path dir) throws IOException {
        Path pack = writePack(dir.resolve("pack.mcpack"), Map.of("pack_icon.png", ICON));

        BedrockPackResources resources = BedrockPackResources.of(pack);
        IoSupplier<InputStream> icon = resources.getRootResource("pack.png");

        assertNotNull(icon);
        try (InputStream first = icon.get()) {
            assertArrayEquals(ICON, first.readAllBytes());
        }
        try (InputStream second = icon.get()) {
            assertArrayEquals(ICON, second.readAllBytes());
        }
        // Windows 上探测用的 ZipFile 若未关闭，句柄会阻止删除；其他平台此断言恒成立
        Files.delete(pack);
    }

    @Test
    void nestedResourcePackIconIsBridged(@TempDir Path dir) throws IOException {
        Path pack = writePack(dir.resolve("pack.mcaddon"),
                Map.of("resource_pack/pack_icon.png", ICON));

        IoSupplier<InputStream> icon = BedrockPackResources.of(pack).getRootResource("pack_icon.png");

        assertNotNull(icon);
        try (InputStream in = icon.get()) {
            assertArrayEquals(ICON, in.readAllBytes());
        }
    }

    @Test
    void missingIconFallsBackToNull(@TempDir Path dir) throws IOException {
        Path pack = writePack(dir.resolve("plain.mcpack"), Map.of());

        assertNull(BedrockPackResources.of(pack).getRootResource("pack.png"));
        Files.delete(pack);
    }

    private static Path writePack(Path file, Map<String, byte[]> entries) throws IOException {
        Map<String, byte[]> all = new LinkedHashMap<>();
        all.put("manifest.json", ("{\"header\":{\"name\":\"test pack\",\"description\":\"d\"}}")
                .getBytes(StandardCharsets.UTF_8));
        all.putAll(entries);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(file))) {
            for (Map.Entry<String, byte[]> entry : all.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
        return file;
    }
}
