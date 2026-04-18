package io.github.tt432.eyelibimporter.addon;

import com.google.gson.JsonObject;

public record BrSoundIndex(
        BedrockResourceValue.ObjectValue entitySounds,
        BedrockResourceValue.ObjectValue blockSounds,
        BedrockResourceValue.ObjectValue interactiveBlockSounds,
        BedrockResourceValue.ObjectValue individualEventSounds,
        BedrockResourceValue.ObjectValue extras
) {
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
