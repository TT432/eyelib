package io.github.tt432.eyelib.animation;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import software.bernie.geckolib3.geo.raw.pojo.FormatVersion;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * @author DustW
 */
@Data
@JsonAdapter(RawAnimation.Serializer.class)
public class RawAnimation {
    private FormatVersion formatVersion;
    private Map<String, AnimationEntry> animations;

    protected static class Serializer implements JsonDeserializer<RawAnimation> {
        @Override
        public RawAnimation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();

            RawAnimation result = new RawAnimation();
            result.formatVersion = context.deserialize(object.get("format_version"), FormatVersion.class);
            JsonElement animationsJson = object.get("animations");
            if (!animationsJson.isJsonNull())
                result.animations = context.deserialize(animationsJson,
                        TypeToken.getParameterized(Map.class, String.class, AnimationEntry.class).getType());
            result.animations.forEach((k, v) -> v.setAnimationName(k));
            return result;
        }
    }
}
