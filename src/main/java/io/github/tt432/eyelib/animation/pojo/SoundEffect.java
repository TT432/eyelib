package io.github.tt432.eyelib.animation.pojo;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import lombok.Data;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author DustW
 */
@Data
@JsonAdapter(SoundEffect.Serializer.class)
public class SoundEffect {
    private final List<ResourceLocation> effect;

    protected static class Serializer implements JsonDeserializer<SoundEffect> {
        @Override
        public SoundEffect deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                return new SoundEffect(Collections.singletonList(new ResourceLocation(json.getAsString())));
            } else if (json.isJsonObject()) {
                return new SoundEffect(Collections.singletonList(new ResourceLocation(json.getAsJsonObject().get("effect").getAsString())));
            } else if (json.isJsonArray()) {
                SoundEffect soundEffect = new SoundEffect(new ArrayList<>());
                json.getAsJsonArray().forEach(j -> soundEffect.effect.add(new ResourceLocation(j.getAsString())));
                return soundEffect;
            }

            throw new JsonParseException("can't parse SoundEffect:" + json);
        }
    }
}
