package io.github.tt432.eyelibimporter.addon;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public record BrSoundDefinitions(
        String formatVersion,
        LinkedHashMap<String, BedrockResourceValue.ObjectValue> soundDefinitions
) {
    public BrSoundDefinitions {
        soundDefinitions = new LinkedHashMap<>(soundDefinitions);
    }

    public static BrSoundDefinitions parse(JsonObject root) {
        String formatVersion = root.get("format_version").getAsString();
        JsonObject definitions = root.getAsJsonObject("sound_definitions");
        LinkedHashMap<String, BedrockResourceValue.ObjectValue> result = new LinkedHashMap<>();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : definitions.entrySet()) {
            BedrockResourceValue value = BedrockResourceValue.fromJsonElement(entry.getValue());
            result.put(entry.getKey(), (BedrockResourceValue.ObjectValue) value);
        }
        return new BrSoundDefinitions(formatVersion, result);
    }

    public Map<String, BedrockResourceValue.ObjectValue> definitionsView() {
        return Map.copyOf(soundDefinitions);
    }
}
