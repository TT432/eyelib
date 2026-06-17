package io.github.tt432.eyelib.importer.addon;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.importer.util.ImporterCodecUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Bedrock 生成规则数据模型。JSON 实际结构为：
 * <pre>
 * {
 *   "format_version": "1.17.0",
 *   "minecraft:spawn_rules": {
 *     "description": { "identifier": "...", "population_control": "..." },
 *     "conditions": [...]
 *   }
 * }
 * </pre>
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record BrSpawnRule(
        String formatVersion,
        String identifier,
        String populationControl,
        List<BedrockResourceValue.ObjectValue> conditions
) {
    /**
     * minecraft:spawn_rules 内层 description 的 CODEC。
     */
    private record SpawnRuleDescription(String identifier, String populationControl) {
        static final Codec<SpawnRuleDescription> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("identifier").forGetter(SpawnRuleDescription::identifier),
                Codec.STRING.fieldOf("population_control").forGetter(SpawnRuleDescription::populationControl)
        ).apply(ins, SpawnRuleDescription::new));
    }

    /**
     * minecraft:spawn_rules 层的 CODEC，包含 description 和 conditions。
     */
    private record SpawnRuleContent(SpawnRuleDescription description, List<BedrockResourceValue.ObjectValue> conditions) {
        static final Codec<SpawnRuleContent> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                SpawnRuleDescription.CODEC.fieldOf("description").forGetter(SpawnRuleContent::description),
                ImporterCodecUtil.OBJECT_VALUE_CODEC.listOf().fieldOf("conditions").forGetter(SpawnRuleContent::conditions)
        ).apply(ins, SpawnRuleContent::new));
    }

    public static final Codec<BrSpawnRule> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("format_version").forGetter(BrSpawnRule::formatVersion),
            SpawnRuleContent.CODEC.fieldOf("minecraft:spawn_rules").forGetter(sr -> new SpawnRuleContent(
                    new SpawnRuleDescription(sr.identifier, sr.populationControl), sr.conditions))
    ).apply(ins, (fmt, content) -> new BrSpawnRule(fmt,
            content.description().identifier(),
            content.description().populationControl(),
            content.conditions())));

    public static BrSpawnRule parse(JsonObject root) {
        String formatVersion = root.get("format_version").getAsString();
        JsonObject spawnRules = root.getAsJsonObject("minecraft:spawn_rules");
        JsonObject description = spawnRules.getAsJsonObject("description");
        String identifier = description.get("identifier").getAsString();
        String populationControl = description.get("population_control").getAsString();
        List<BedrockResourceValue.ObjectValue> conditions = new ArrayList<>();
        for (var element : spawnRules.getAsJsonArray("conditions")) {
            conditions.add((BedrockResourceValue.ObjectValue) BedrockResourceValue.fromJsonElement(element));
        }
        return new BrSpawnRule(formatVersion, identifier, populationControl, conditions);
    }
}
