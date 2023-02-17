package io.github.tt432.eyelib.common.bedrock.animation.pojo;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * @author DustW
 */
@Data
@JsonAdapter(ParticleEffect.class)
public class ParticleEffect implements JsonDeserializer<ParticleEffect> {
    private final String id;
    @Nullable
    private final String locator;

    @Override
    public ParticleEffect deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonPrimitive()) {
            return new ParticleEffect(json.getAsString(), null);
        } else {
            JsonObject object = json.getAsJsonObject();
            return new ParticleEffect(object.get("effect").getAsString(), object.get("locator").getAsString());
        }
    }
}
