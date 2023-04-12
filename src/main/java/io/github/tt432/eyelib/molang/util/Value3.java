package io.github.tt432.eyelib.molang.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.mojang.math.Vector3f;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.util.Axis;
import lombok.Data;
import net.minecraft.world.phys.Vec3;

import java.io.Serializable;
import java.lang.reflect.Type;

/**
 * @author DustW
 */
@Data
@JsonAdapter(Value3.Serializer.class)
public class Value3 implements Serializable {
    private final MolangValue x;
    private final MolangValue y;
    private final MolangValue z;

    public MolangValue get(Axis axis) {
        return switch (axis) {
            case X -> x;
            case Y -> y;
            case Z -> z;
        };
    }

    public void evaluateWithCache(String name, MolangVariableScope scope) {
        x.evaluateWithCache(name + "x", scope);
        y.evaluateWithCache(name + "y", scope);
        z.evaluateWithCache(name + "z", scope);
    }

    public Vec3 evaluate(MolangVariableScope scope) {
        return new Vec3(
                x.evaluate(scope),
                y.evaluate(scope),
                z.evaluate(scope)
        );
    }

    public Vec3 fromCache(String name, MolangVariableScope scope) {
        return new Vec3(
                scope.getValue(name + "x"),
                scope.getValue(name + "y"),
                scope.getValue(name + "z")
        );
    }

    public Vector3f toVec3f(MolangVariableScope scope) {
        return new Vector3f((float) x.evaluate(scope), (float) y.evaluate(scope), (float) z.evaluate(scope));
    }

    protected static class Serializer implements JsonDeserializer<Value3> {
        @Override
        public Value3 deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            MolangValue[] deserialize = context.deserialize(json, MolangValue[].class);
            if (deserialize.length != 3)
                throw new JsonParseException("Value3 must be IValue[3]");
            return new Value3(deserialize[0], deserialize[1], deserialize[2]);
        }
    }
}
