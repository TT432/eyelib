package io.github.tt432.eyelib.common.bedrock.animation.pojo;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import lombok.Data;

import java.lang.reflect.Type;

/**
 * @author DustW
 */
@Data
@JsonAdapter(Timestamp.Serializer.class)
public class Timestamp {
    public static final Timestamp ZERO = new Timestamp(0);

    private final double tick;

    public static Timestamp valueOf(String s) {
        return new Timestamp(Double.parseDouble(s) * 20);
    }

    protected static class Serializer implements JsonDeserializer<Timestamp>, JsonSerializer<Timestamp> {
        @Override
        public Timestamp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Timestamp.valueOf(json.getAsString());
        }

        @Override
        public JsonElement serialize(Timestamp src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(String.valueOf(src.tick / 20D));
        }
    }
}
