package io.github.tt432.eyelib.util.math;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import lombok.Data;
import io.github.tt432.eyelib.util.math.molang.math.IValue;
import io.github.tt432.eyelib.util.Axis;

import java.lang.reflect.Type;

/**
 * @author DustW
 */
@Data
@JsonAdapter(Value3.Serializer.class)
public class Value3 {
    private final IValue x;
    private final IValue y;
    private final IValue z;

    public IValue get(Axis axis) {
        return switch (axis){
            case X -> x;
            case Y -> y;
            case Z -> z;
        };
    }

    protected static class Serializer implements JsonDeserializer<Value3> {
        @Override
        public Value3 deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            IValue[] deserialize = context.deserialize(json, IValue[].class);
            if (deserialize.length != 3)
                throw new JsonParseException("Value3 must be IValue[3]");
            return new Value3(deserialize[0], deserialize[1], deserialize[2]);
        }
    }
}
