package io.github.tt432.eyelib.importer.addon;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LangAssetRegistry} 的 stage 整体替换语义、语言 code 提取与
 * entriesByLanguageCode 合并语义。
 *
 * @author TT432
 */
class LangAssetRegistryTest {

    @Test
    void stageReplacesPreviousContent() {
        LangAssetRegistry.stageLangFiles(Map.of(
                "texts/en_US.lang", BrLanguageFile.parse("a=1\nb=2")));
        LangAssetRegistry.stageLangFiles(Map.of(
                "texts/zh_CN.lang", BrLanguageFile.parse("c=3")));

        assertEquals(Map.of("zh_cn", Map.of("c", "3")),
                LangAssetRegistry.entriesByLanguageCode());
        LangAssetRegistry.stageLangFiles(Map.of());
        assertTrue(LangAssetRegistry.entriesByLanguageCode().isEmpty());
    }

    @Test
    void languageCodeLowercasesAndStripsSuffix() {
        LangAssetRegistry.stageLangFiles(Map.of(
                "texts/en_US.lang", BrLanguageFile.parse("a=1"),
                "texts/zh_CN.lang", BrLanguageFile.parse("b=2")));

        assertEquals(Map.of("en_us", Map.of("a", "1"), "zh_cn", Map.of("b", "2")),
                LangAssetRegistry.entriesByLanguageCode());
        assertEquals(Map.of("en_us", Map.of("a", "1"), "zh_cn", Map.of("b", "2")).keySet(),
                LangAssetRegistry.knownLanguageCodes());
        LangAssetRegistry.stageLangFiles(Map.of());
    }

    @Test
    void sameCodeFromMultipleFilesLaterOverridesEarlier() {
        Map<String, BrLanguageFile> staged = new LinkedHashMap<>();
        staged.put("texts/en_US.lang", BrLanguageFile.parse("shared=first\na=1"));
        staged.put("override/texts/en_us.lang", BrLanguageFile.parse("shared=second\nb=2"));
        LangAssetRegistry.stageLangFiles(staged);

        Map<String, Map<String, String>> merged = LangAssetRegistry.entriesByLanguageCode();
        assertEquals(1, merged.size());
        // 后并覆盖先存：override 文件的 shared 胜出，异键并集
        assertEquals(Map.of("shared", "second", "a", "1", "b", "2"), merged.get("en_us"));
        LangAssetRegistry.stageLangFiles(Map.of());
    }
}
