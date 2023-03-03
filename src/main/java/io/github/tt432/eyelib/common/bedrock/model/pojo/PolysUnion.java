package io.github.tt432.eyelib.common.bedrock.model.pojo;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import java.io.IOException;
import java.lang.reflect.Type;

@JsonAdapter(PolysUnion.Serializer.class)
public class PolysUnion {
    public double[][][] doubleArrayArrayArrayValue;
    public PolysEnum enumValue;

    protected static class Serializer implements JsonDeserializer<PolysUnion> {
        @Override
        public PolysUnion deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            PolysUnion result = new PolysUnion();

            if (json.isJsonArray()) {
                result.doubleArrayArrayArrayValue = context.deserialize(json, double[][][].class);
            } else if (json.isJsonPrimitive()) {
                try {
                    result.enumValue = PolysEnum.forValue(json.getAsString());
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }

            return result;
        }
    }
}
