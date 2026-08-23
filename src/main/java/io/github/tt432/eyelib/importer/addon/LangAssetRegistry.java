package io.github.tt432.eyelib.importer.addon;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Bedrock 语言文件注册表（随 BedrockAddonRuntimeBridge 整体替换）：
 * texts/*.lang 的「包内相对路径 → {@link BrLanguageFile}」。消费方为内存语言包
 * {@code AddonLangPack}（合成 assets/eyelibaddon/lang/&lt;code&gt;.json 接入 MC 语言系统）。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LangAssetRegistry {
    private static Map<String, BrLanguageFile> langFiles = Map.of();

    /** 随 replaceFromResourcePack 一并替换（语义与 SoundAssetRegistry.stageSounds 一致）。 */
    public static void stageLangFiles(Map<String, BrLanguageFile> files) {
        // LinkedHashMap 快照保留迭代序——同 code 多文件「后并覆盖先存」依赖此序（Map.copyOf 无序）
        langFiles = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(files));
    }

    /**
     * 按语言 code 合并的条目视图：code = 文件名小写去 .lang 后缀（texts/en_US.lang → en_us）。
     * 多文件同 code 时按 stage 迭代序「后并覆盖先存」（后文件同键覆盖前文件，与 BedrockAddon
     * 跨包合并的 vanilla 优先级语义一致）。
     */
    public static Map<String, Map<String, String>> entriesByLanguageCode() {
        Map<String, Map<String, String>> merged = new LinkedHashMap<>();
        for (Map.Entry<String, BrLanguageFile> e : langFiles.entrySet()) {
            String code = languageCodeOf(e.getKey());
            if (code == null) {
                continue;
            }
            merged.computeIfAbsent(code, k -> new LinkedHashMap<>())
                    .putAll(e.getValue().entriesView());
        }
        return merged;
    }

    /** 已知语言 code 全集（en_us 等，小写）。 */
    public static Set<String> knownLanguageCodes() {
        return entriesByLanguageCode().keySet();
    }

    /**
     * 包内相对路径 → 语言 code：取最后一段文件名，去 .lang 后缀并小写。
     * 非 .lang 结尾返回 null（防御性；loader 已过滤）。
     */
    static String languageCodeOf(String relativePath) {
        int slash = relativePath.lastIndexOf('/');
        String fileName = slash >= 0 ? relativePath.substring(slash + 1) : relativePath;
        if (!fileName.endsWith(".lang")) {
            return null;
        }
        return fileName.substring(0, fileName.length() - ".lang".length())
                .toLowerCase(Locale.ROOT);
    }
}
