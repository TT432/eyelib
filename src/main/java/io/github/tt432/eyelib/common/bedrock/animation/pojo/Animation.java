package io.github.tt432.eyelib.common.bedrock.animation.pojo;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import io.github.tt432.eyelib.common.bedrock.FormatVersion;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * @author DustW
 */
@Data
@JsonAdapter(Animation.Serializer.class)
public class Animation {
    private FormatVersion formatVersion;
    private Map<String, SingleAnimation> animations;

    protected static class Serializer implements JsonDeserializer<Animation> {
        @Override
        public Animation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();

            Animation result = new Animation();
            result.formatVersion = context.deserialize(object.get("format_version"), FormatVersion.class);
            JsonElement animationsJson = object.get("animations");
            if (!animationsJson.isJsonNull())
                result.animations = context.deserialize(animationsJson,
                        TypeToken.getParameterized(Map.class, String.class, SingleAnimation.class).getType());
            result.animations.forEach((k, v) -> v.setAnimationName(k));
            return result;
        }
    }
}
