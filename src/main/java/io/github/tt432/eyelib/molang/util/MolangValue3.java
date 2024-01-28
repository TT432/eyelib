package io.github.tt432.eyelib.molang.util;

import com.google.gson.JsonArray;
import io.github.tt432.eyelib.molang.MolangSystemScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.math.Axis;

/**
 * @author TT432
 */
public record MolangValue3(
        MolangValue x,
        MolangValue y,
        MolangValue z
) {

    public float getX() {
        return x.eval();
    }

    public float getY() {
        return y.eval();
    }

    public float getZ() {
        return z.eval();
    }

    public static MolangValue3 parse(MolangSystemScope scope, JsonArray array) {
        return new MolangValue3(
                MolangValue.parse(scope, array.get(0).getAsString()),
                MolangValue.parse(scope, array.get(1).getAsString()),
                MolangValue.parse(scope, array.get(2).getAsString())
        );
    }

    public float get(Axis axis) {
        return switch (axis) {
            case X -> getX();
            case Y -> getY();
            case Z -> getZ();
        };
    }
}
