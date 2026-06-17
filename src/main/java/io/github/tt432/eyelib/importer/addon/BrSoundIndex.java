package io.github.tt432.eyelib.importer.addon;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.importer.util.ImporterCodecUtil;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BrSoundIndex(
        BedrockResourceValue.ObjectValue entitySounds,
        BedrockResourceValue.ObjectValue blockSounds,
        BedrockResourceValue.ObjectValue interactiveBlockSounds,
        BedrockResourceValue.ObjectValue individualEventSounds,
        BedrockResourceValue.ObjectValue extras
) {
    public static final Codec<BrSoundIndex> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            ImporterCodecUtil.OBJECT_VALUE_CODEC.optionalFieldOf("entity_sounds", null).forGetter(BrSoundIndex::entitySounds),
            ImporterCodecUtil.OBJECT_VALUE_CODEC.optionalFieldOf("block_sounds", null).forGetter(BrSoundIndex::blockSounds),
            ImporterCodecUtil.OBJECT_VALUE_CODEC.optionalFieldOf("interactive_block_sounds", null).forGetter(BrSoundIndex::interactiveBlockSounds),
            ImporterCodecUtil.OBJECT_VALUE_CODEC.optionalFieldOf("individual_event_sounds", null).forGetter(BrSoundIndex::individualEventSounds),
            ImporterCodecUtil.OBJECT_VALUE_CODEC.fieldOf("extras").forGetter(BrSoundIndex::extras)
    ).apply(ins, BrSoundIndex::new));

    public static BrSoundIndex parse(JsonObject root) {
        BedrockResourceValue.ObjectValue entitySounds = getObject(root, "entity_sounds");
        BedrockResourceValue.ObjectValue blockSounds = getObject(root, "block_sounds");
        BedrockResourceValue.ObjectValue interactiveBlockSounds = getObject(root, "interactive_block_sounds");
        BedrockResourceValue.ObjectValue individualEventSounds = getObject(root, "individual_event_sounds");
        JsonObject extras = root.deepCopy();
        extras.remove("entity_sounds");
        extras.remove("block_sounds");
        extras.remove("interactive_block_sounds");
        extras.remove("individual_event_sounds");
        return new BrSoundIndex(entitySounds, blockSounds, interactiveBlockSounds, individualEventSounds,
                (BedrockResourceValue.ObjectValue) BedrockResourceValue.fromJsonElement(extras));
    }

    private static BedrockResourceValue.ObjectValue getObject(JsonObject root, String key) {
        if (!root.has(key)) {
            return null;
        }
        return (BedrockResourceValue.ObjectValue) BedrockResourceValue.fromJsonElement(root.get(key));
    }
}
