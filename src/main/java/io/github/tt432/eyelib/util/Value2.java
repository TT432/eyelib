package io.github.tt432.eyelib.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import io.github.tt432.eyelib.util.molang.MolangValue;
import lombok.Data;

import java.lang.reflect.Type;

/**
 * @author DustW
 */
@Data
@JsonAdapter(Value2.Serializer.class)
public class Value2 {
    private final MolangValue x;
    private final MolangValue y;

    public MolangValue get(Axis axis) {
        return switch (axis){
            case X -> x;
            case Y -> y;
            default -> throw new IllegalArgumentException("Value2 not have z axis");
        };
    }

    protected static class Serializer implements JsonDeserializer<Value2> {
        @Override
        public Value2 deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            MolangValue[] deserialize = context.deserialize(json, MolangValue[].class);
            if (deserialize.length != 2)
                throw new JsonParseException("Value3 must be IValue[3]");
            return new Value2(deserialize[0], deserialize[1]);
        }
    }
}
