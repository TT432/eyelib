package io.github.tt432.eyelib.importer.addon;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.importer.util.ImporterCodecUtil;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BrBehaviorEntityFile(
        String formatVersion,
        String identifier,
        BedrockResourceValue.ObjectValue description,
        BedrockResourceValue.ObjectValue componentGroups,
        BedrockResourceValue.ObjectValue components,
        BedrockResourceValue.ObjectValue events,
        BedrockResourceValue.ObjectValue extras
) {
    public static final Codec<BrBehaviorEntityFile> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("format_version").forGetter(BrBehaviorEntityFile::formatVersion),
            Codec.STRING.fieldOf("identifier").forGetter(BrBehaviorEntityFile::identifier),
            ImporterCodecUtil.OBJECT_VALUE_CODEC.fieldOf("description").forGetter(BrBehaviorEntityFile::description),
            ImporterCodecUtil.OBJECT_VALUE_CODEC.optionalFieldOf("component_groups", null).forGetter(BrBehaviorEntityFile::componentGroups),
            ImporterCodecUtil.OBJECT_VALUE_CODEC.optionalFieldOf("components", null).forGetter(BrBehaviorEntityFile::components),
            ImporterCodecUtil.OBJECT_VALUE_CODEC.optionalFieldOf("events", null).forGetter(BrBehaviorEntityFile::events),
            ImporterCodecUtil.OBJECT_VALUE_CODEC.fieldOf("extras").forGetter(BrBehaviorEntityFile::extras)
    ).apply(ins, BrBehaviorEntityFile::new));

    public static BrBehaviorEntityFile parse(JsonObject root) {
        String formatVersion = root.get("format_version").getAsString();
        JsonObject entityRoot = root.getAsJsonObject("minecraft:entity");
        JsonObject description = entityRoot.getAsJsonObject("description");
        String identifier = description.get("identifier").getAsString();
        JsonObject extras = entityRoot.deepCopy();
        extras.remove("description");
        extras.remove("component_groups");
        extras.remove("components");
        extras.remove("events");
        return new BrBehaviorEntityFile(
                formatVersion,
                identifier,
                (BedrockResourceValue.ObjectValue) BedrockResourceValue.fromJsonElement(description),
                getObject(entityRoot, "component_groups"),
                getObject(entityRoot, "components"),
                getObject(entityRoot, "events"),
                (BedrockResourceValue.ObjectValue) BedrockResourceValue.fromJsonElement(extras)
        );
    }

    private static BedrockResourceValue.ObjectValue getObject(JsonObject root, String key) {
        if (!root.has(key)) {
            return null;
        }
        return (BedrockResourceValue.ObjectValue) BedrockResourceValue.fromJsonElement(root.get(key));
    }
}
