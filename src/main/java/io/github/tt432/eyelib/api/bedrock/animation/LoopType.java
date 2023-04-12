package io.github.tt432.eyelib.api.bedrock.animation;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import lombok.Getter;

import java.lang.reflect.Type;
import java.util.Locale;

@JsonAdapter(LoopType.Serializer.class)
public enum LoopType {
    LOOP(true),
    PLAY_ONCE,
    HOLD_ON_LAST_FRAME;

    @Getter
    private final boolean looping;

    LoopType(boolean looping) {
        this.looping = looping;
    }

    LoopType() {
        this(false);
    }

    static class Serializer implements JsonDeserializer<LoopType> {
        @Override
        public LoopType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || !json.isJsonPrimitive()) {
                return PLAY_ONCE;
            }

            JsonPrimitive primitive = json.getAsJsonPrimitive();

            if (primitive.isBoolean()) {
                return primitive.getAsBoolean() ? LOOP : PLAY_ONCE;
            }

            if (primitive.isString()) {
                String string = primitive.getAsString();

                if (string.equalsIgnoreCase("false")) {
                    return PLAY_ONCE;
                }

                if (string.equalsIgnoreCase("true")) {
                    return LOOP;
                }

                try {
                    return valueOf(string.toUpperCase(Locale.ROOT));
                } catch (Exception ignored) {
                    // nothing
                }
            }

            return PLAY_ONCE;
        }
    }
}
