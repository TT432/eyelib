package io.github.tt432.eyelibimporter.addon;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibimporter.util.ImporterCodecUtil;

import java.util.ArrayList;
import java.util.List;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BrSpawnRule(
        String formatVersion,
        String identifier,
        String populationControl,
        List<BedrockResourceValue.ObjectValue> conditions
) {
    public static final Codec<BrSpawnRule> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("format_version").forGetter(BrSpawnRule::formatVersion),
            Codec.STRING.fieldOf("identifier").forGetter(BrSpawnRule::identifier),
            Codec.STRING.fieldOf("population_control").forGetter(BrSpawnRule::populationControl),
            ImporterCodecUtil.OBJECT_VALUE_CODEC.listOf().fieldOf("conditions").forGetter(BrSpawnRule::conditions)
    ).apply(ins, BrSpawnRule::new));

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
