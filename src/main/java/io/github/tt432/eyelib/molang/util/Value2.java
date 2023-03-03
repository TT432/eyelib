package io.github.tt432.eyelib.molang.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.util.Axis;
import io.github.tt432.eyelib.util.math.Vec2d;
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
        return switch (axis) {
            case X -> x;
            case Y -> y;
            default -> throw new IllegalArgumentException("Value2 not have z axis");
        };
    }

    public void evaluateWithCache(String name, MolangVariableScope scope) {
        x.evaluateWithCache(name + "x", scope);
        y.evaluateWithCache(name + "y", scope);
    }

    public Vec2d evaluate(MolangVariableScope scope) {
        return new Vec2d(
                x.evaluate(scope),
                y.evaluate(scope)
        );
    }

    public Vec2d fromCache(String name, MolangVariableScope scope) {
        return new Vec2d(
                scope.getValue(name + "x"),
                scope.getValue(name + "y")
        );
    }

    protected static class Serializer implements JsonDeserializer<Value2> {
        @Override
        public Value2 deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            MolangValue[] deserialize = context.deserialize(json, MolangValue[].class);
            if (deserialize.length != 2)
                throw new JsonParseException("Value2 must be IValue[2]");
            return new Value2(deserialize[0], deserialize[1]);
        }
    }
}
