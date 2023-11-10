package io.github.tt432.eyelib.molang.util;

import com.google.gson.JsonArray;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.math.Axis;
import lombok.AllArgsConstructor;
import org.joml.Vector3f;

/**
 * @author TT432
 */
@AllArgsConstructor
public class MolangValue3 {
    final MolangValue x;
    final MolangValue y;
    final MolangValue z;

    public MolangValue3 copy(MolangScope scope) {
        return new MolangValue3(
                x.copy(scope),
                y.copy(scope),
                z.copy(scope)
        );
    }

    public float getX() {
        return x.eval();
    }

    public float getY() {
        return y.eval();
    }

    public float getZ() {
        return z.eval();
    }

    public Vector3f toVec3f() {
        return new Vector3f(
                x.eval(),
                y.eval(),
                z.eval()
        );
    }

    public static MolangValue3 parse(MolangScope scope, JsonArray array) {
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
