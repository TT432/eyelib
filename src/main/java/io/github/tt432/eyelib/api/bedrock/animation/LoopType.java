package io.github.tt432.eyelib.api.bedrock.animation;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.Locale;

@JsonAdapter(LoopType.Serializer.class)
public interface LoopType {

    boolean isRepeatingAfterEnd();

    enum Impl implements LoopType {
        LOOP(true),
        PLAY_ONCE,
        HOLD_ON_LAST_FRAME;

        private final boolean looping;

        Impl(boolean looping) {
            this.looping = looping;
        }

        Impl() {
            this(false);
        }

        @Override
        public boolean isRepeatingAfterEnd() {
            return this.looping;
        }
    }

    class Serializer implements JsonDeserializer<LoopType> {
        @Override
        public LoopType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return fromJson(json);
        }
    }

    static LoopType fromJson(JsonElement json) {
        if (json == null || !json.isJsonPrimitive()) {
            return Impl.PLAY_ONCE;
        }

        JsonPrimitive primitive = json.getAsJsonPrimitive();

        if (primitive.isBoolean()) {
            return primitive.getAsBoolean() ? Impl.LOOP : Impl.PLAY_ONCE;
        }

        if (primitive.isString()) {
            String string = primitive.getAsString();

            if (string.equalsIgnoreCase("false")) {
                return Impl.PLAY_ONCE;
            }

            if (string.equalsIgnoreCase("true")) {
                return Impl.LOOP;
            }

            try {
                return Impl.valueOf(string.toUpperCase(Locale.ROOT));
            } catch (Exception ex) {
            }
        }

        return Impl.PLAY_ONCE;
    }
}
