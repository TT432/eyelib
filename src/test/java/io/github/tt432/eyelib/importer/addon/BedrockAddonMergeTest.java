package io.github.tt432.eyelib.importer.addon;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BedrockAddon#merge} 跨包合并语义：vanilla 资源包优先级顺序（列表靠后 = 高优先级）
 * 下同键后者覆盖前者，异键并集，告警保留。
 *
 * @author TT432
 */
class BedrockAddonMergeTest {

    @Test
    void mergeEmptyListReturnsEmptyView() {
        BedrockAddon merged = BedrockAddon.merge(List.of());

        assertTrue(merged.packs().isEmpty());
        assertTrue(merged.warnings().isEmpty());
        assertTrue(merged.aggregate().resourcePack().soundFiles().isEmpty());
        assertTrue(merged.aggregate().resourcePack().clientEntities().isEmpty());
    }

    @Test
    void mergeSingleReturnsSameInstance() {
        BedrockAddon addon = addonOf(pack("a", new LinkedHashMap<>()));

        assertSame(addon, BedrockAddon.merge(List.of(addon)));
    }

    @Test
    void laterAddonOverridesEarlierOnSameKey() {
        LinkedHashMap<String, BedrockBinaryAsset> soundsA = new LinkedHashMap<>();
        soundsA.put("sounds/a.ogg", asset("A1"));
        LinkedHashMap<String, BedrockBinaryAsset> soundsB = new LinkedHashMap<>();
        soundsB.put("sounds/a.ogg", asset("B1"));
        soundsB.put("sounds/b.ogg", asset("B2"));

        BedrockAddon merged = BedrockAddon.merge(List.of(
                addonOf(pack("a", soundsA)),
                addonOf(pack("b", soundsB))));

        var sounds = merged.aggregate().resourcePack().soundFiles();
        assertEquals(2, sounds.size());
        assertEquals("B1", new String(sounds.get("sounds/a.ogg").bytes()));
        assertEquals("B2", new String(sounds.get("sounds/b.ogg").bytes()));
        // 键序：先包的键保留原位，后包新键追加
        assertEquals(List.of("sounds/a.ogg", "sounds/b.ogg"), List.copyOf(sounds.keySet()));
    }

    @Test
    void warningsArePreservedInPackOrder() {
        BedrockAddon a = addonOf(pack("a", new LinkedHashMap<>()));
        BedrockAddon b = addonOf(pack("b", new LinkedHashMap<>()));

        BedrockAddon merged = BedrockAddon.merge(List.of(a, b));

        assertEquals(2, merged.warnings().size());
        assertEquals("a", merged.warnings().get(0).packSource());
        assertEquals("b", merged.warnings().get(1).packSource());
    }

    // ---------- fixtures ----------

    private static BedrockBinaryAsset asset(String content) {
        return new BedrockBinaryAsset(".ogg", content.getBytes());
    }

    private static BedrockAddonPack pack(String sourceName, LinkedHashMap<String, BedrockBinaryAsset> soundFiles) {
        BedrockPackManifest manifest = BedrockPackManifest.parse(JsonParser.parseString(
                "{\"format_version\":2,"
                        + "\"header\":{\"name\":\"" + sourceName + "\",\"uuid\":\"u-" + sourceName + "\"},"
                        + "\"modules\":[{\"type\":\"resources\",\"uuid\":\"m-" + sourceName + "\"}]}"
        ).getAsJsonObject());
        return new BedrockAddonPack(
                sourceName, manifest, null,
                new LinkedHashMap<>(), // animationFiles
                new LinkedHashMap<>(), // animationControllerFiles
                new LinkedHashMap<>(), // clientEntityFiles
                new LinkedHashMap<>(), // attachableFiles
                new LinkedHashMap<>(), // modelFiles
                new LinkedHashMap<>(), // textures
                new LinkedHashMap<>(), // soundIndexFiles
                new LinkedHashMap<>(), // soundDefinitionFiles
                new LinkedHashMap<>(), // languageFiles
                new LinkedHashMap<>(), // fogFiles
                new LinkedHashMap<>(), // uiFiles
                new LinkedHashMap<>(), // behaviorEntityFiles
                soundFiles,            // soundFiles
                new LinkedHashMap<>(), // textureIndexFiles
                new LinkedHashMap<>(), // textureMetadataFiles
                new LinkedHashMap<>(), // renderControllerFiles
                new LinkedHashMap<>(), // particleFiles
                new LinkedHashMap<>(), // materialFiles
                new LinkedHashMap<>(), // spawnRulesFiles
                new LinkedHashMap<>(), // lootTableFiles
                new LinkedHashMap<>(), // itemFiles
                new LinkedHashMap<>(), // blockFiles
                new LinkedHashMap<>(), // recipeFiles
                new LinkedHashMap<>(), // tradeFiles
                new LinkedHashMap<>(), // unmanagedResources
                List.of(new BedrockAddonWarning(BedrockAddonWarningSeverity.INFO,
                        BedrockAddonWarningCode.values()[0], sourceName, null, "fixture")),
                null, null);
    }

    private static BedrockAddon addonOf(BedrockAddonPack pack) {
        List<BedrockAddonWarning> warnings = new ArrayList<>(pack.warnings());
        return new BedrockAddon(
                List.of(pack), warnings, new LinkedHashMap<>(),
                BedrockAddonAggregate.fromPacks(List.of(pack), warnings));
    }
}
