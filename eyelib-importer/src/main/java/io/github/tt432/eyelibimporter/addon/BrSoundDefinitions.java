package io.github.tt432.eyelibimporter.addon;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibimporter.util.ImporterCodecUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BrSoundDefinitions(
        String formatVersion,
        LinkedHashMap<String, BedrockResourceValue.ObjectValue> soundDefinitions
) {
    public static final Codec<BrSoundDefinitions> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("format_version").forGetter(BrSoundDefinitions::formatVersion),
            Codec.unboundedMap(Codec.STRING, ImporterCodecUtil.OBJECT_VALUE_CODEC)
                    .xmap(LinkedHashMap::new, m -> m)
                    .fieldOf("sound_definitions")
                    .forGetter(BrSoundDefinitions::soundDefinitions)
    ).apply(ins, BrSoundDefinitions::new));

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
