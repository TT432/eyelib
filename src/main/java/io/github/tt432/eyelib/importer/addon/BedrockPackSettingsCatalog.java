package io.github.tt432.eyelib.importer.addon;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 面向 UI/运行时的包设置目录：从 .mcpack/.mcaddon 文件解析出
 * subpacks、类型化 settings 与 texts/en_US.lang 本地化表。
 * <p>
 * 与 {@link BedrockAddonLoader} 的全量内容解析不同，这里只读管理面信息
 * （manifest + lang），供资源包列表的设置界面与齿轮按钮显隐判定使用。
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record BedrockPackSettingsCatalog(
        /** 包键（资源包文件名，与 vanilla pack id "file/&lt;name&gt;" 对应）。 */
        String packKey,
        List<BedrockPackManifest.Subpack> subpacks,
        List<BedrockPackSetting> settings,
        /** texts/en_US.lang 的 key→value 表（文件缺失为空表）。 */
        Map<String, String> lang
) {
    /** 解析包文件的设置目录；文件不可读/无 manifest 返回 null。 */
    public static @Nullable BedrockPackSettingsCatalog load(Path file) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry manifestEntry = zip.getEntry("manifest.json");
            if (manifestEntry == null) {
                manifestEntry = zip.getEntry("resource_pack/manifest.json");
            }
            if (manifestEntry == null) {
                return null;
            }
            BedrockPackManifest manifest;
            try (InputStream in = zip.getInputStream(manifestEntry)) {
                manifest = BedrockPackManifest.parse(JsonParser.parseString(
                        new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject());
            }
            String prefix = zip.getEntry("manifest.json") != null ? "" : "resource_pack/";
            Map<String, String> lang = readLang(zip, prefix + "texts/en_US.lang");
            return new BedrockPackSettingsCatalog(
                    file.getFileName().toString(),
                    manifest.subpacks(),
                    BedrockPackSetting.parseList(manifest.settings()),
                    lang);
        } catch (Exception e) {
            return null;
        }
    }

    /** 是否有任何可配置内容（决定资源包条目是否显示设置按钮）。 */
    public boolean hasSettings() {
        return !subpacks.isEmpty()
                || settings.stream().anyMatch(s -> s.type() != BedrockPackSetting.Type.LABEL);
    }

    /** 本地化解析：lang 表命中取值，否则原样返回（含 § 格式码，vanilla 字体可渲染）。 */
    public String displayText(String keyOrText) {
        return lang.getOrDefault(keyOrText, keyOrText);
    }

    private static Map<String, String> readLang(ZipFile zip, String path) {
        ZipEntry entry = zip.getEntry(path);
        if (entry == null) {
            return Map.of();
        }
        try (InputStream in = zip.getInputStream(entry)) {
            return BedrockLangFile.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Map.of();
        }
    }
}
