package io.github.tt432.eyelib.api.animation;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.Locale;

@JsonAdapter(LoopType.Serializer.class)
public interface LoopType {

    boolean isRepeatingAfterEnd();

    enum EDefaultLoopTypes implements LoopType {
        LOOP(true),
        PLAY_ONCE,
        HOLD_ON_LAST_FRAME;

        private final boolean looping;

        EDefaultLoopTypes(boolean looping) {
            this.looping = looping;
        }

        EDefaultLoopTypes() {
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
            return EDefaultLoopTypes.PLAY_ONCE;
        }

        JsonPrimitive primitive = json.getAsJsonPrimitive();

        if (primitive.isBoolean()) {
            return primitive.getAsBoolean() ? EDefaultLoopTypes.LOOP : EDefaultLoopTypes.PLAY_ONCE;
        }

        if (primitive.isString()) {
            String string = primitive.getAsString();

            if (string.equalsIgnoreCase("false")) {
                return EDefaultLoopTypes.PLAY_ONCE;
            }

            if (string.equalsIgnoreCase("true")) {
                return EDefaultLoopTypes.LOOP;
            }

            try {
                return EDefaultLoopTypes.valueOf(string.toUpperCase(Locale.ROOT));
            } catch (Exception ex) {
            }
        }

        return EDefaultLoopTypes.PLAY_ONCE;
    }
}
