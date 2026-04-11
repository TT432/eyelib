package io.github.tt432.eyelibmolang;

import com.mojang.serialization.Codec;
import org.joml.Vector4f;

import java.util.List;

/**
 * @author TT432
 */
public record MolangValue4(
        MolangValue x,
        MolangValue y,
        MolangValue z,
        MolangValue w
) {
    public static final MolangValue4 ZERO = new MolangValue4(MolangValue.ZERO, MolangValue.ZERO, MolangValue.ZERO, MolangValue.ZERO);

    public static final MolangValue4 AXIS_X = new MolangValue4(MolangValue.ONE, MolangValue.ZERO, MolangValue.ZERO, MolangValue.ZERO);
    public static final MolangValue4 AXIS_Y = new MolangValue4(MolangValue.ZERO, MolangValue.ONE, MolangValue.ZERO, MolangValue.ZERO);
    public static final MolangValue4 AXIS_Z = new MolangValue4(MolangValue.ZERO, MolangValue.ZERO, MolangValue.ONE, MolangValue.ZERO);
    public static final MolangValue4 AXIS_W = new MolangValue4(MolangValue.ZERO, MolangValue.ZERO, MolangValue.ZERO, MolangValue.ONE);

    public static final Codec<MolangValue4> CODEC = MolangCodecs.fixedSizeList(MolangValue.CODEC, 4)
            .xmap(values -> new MolangValue4(values.get(0), values.get(1), values.get(2), values.get(3)), value -> List.of(value.x, value.y, value.z, value.w));

    public float getX(MolangScope scope) {
        return x.eval(scope);
    }

    public float getY(MolangScope scope) {
        return y.eval(scope);
    }

    public float getZ(MolangScope scope) {
        return z.eval(scope);
    }

    public float getW(MolangScope scope) {
        return w.eval(scope);
    }

    public Vector4f eval(MolangScope scope) {
        return new Vector4f(getX(scope), getY(scope), getZ(scope), getW(scope));
    }
}
