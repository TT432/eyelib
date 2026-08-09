package io.github.tt432.eyelib.importer.addon;

import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bedrock 音效资产注册表（随 BedrockAddonRuntimeBridge 整体替换）：
 * sound_definitions 的「音效 id → 定义 JSON」（sounds/category）与 sounds/ 目录的
 * 音频字节（BedrockBinaryAsset）。消费方：缺失引用检查（D2）与音效预览播放（D5）。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SoundAssetRegistry {
    private static Map<String, JsonObject> definitions = Map.of();
    private static Map<String, BedrockBinaryAsset> files = Map.of();

    /** 随 replaceFromResourcePack 一并替换（语义与 AnimationAssetRegistry.stageSchemas 一致）。 */
    public static void stageSounds(Map<String, BrSoundDefinitions> definitionFiles,
                                   Map<String, BedrockBinaryAsset> soundFiles) {
        Map<String, JsonObject> defs = new LinkedHashMap<>();
        for (BrSoundDefinitions file : definitionFiles.values()) {
            file.soundDefinitions().forEach((id, obj) ->
                    defs.putIfAbsent(id, obj.toJsonElement().getAsJsonObject()));
        }
        definitions = Map.copyOf(defs);
        files = Map.copyOf(soundFiles);
    }

    /** 已定义音效 id 全集（缺失引用判定）。 */
    public static Set<String> knownSoundIds() {
        return definitions.keySet();
    }

    /** 音效定义 JSON（含 sounds 数组与 category），供 sounds.json 合成。 */
    public static Optional<JsonObject> definition(String soundId) {
        return Optional.ofNullable(definitions.get(soundId));
    }

    /** 音频文件字节视图：键为包内有效路径（sounds/xxx.ogg）。 */
    public static Map<String, BedrockBinaryAsset> filesView() {
        return files;
    }
}
