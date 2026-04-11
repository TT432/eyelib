package io.github.tt432.eyelibmolang;

import com.mojang.serialization.Codec;
import org.joml.Vector2f;

import java.util.List;

/**
 * @author TT432
 */
public record MolangValue2(
        MolangValue x,
        MolangValue y
) {
    public static final MolangValue2 ZERO = new MolangValue2(MolangValue.ZERO, MolangValue.ZERO);
    public static final MolangValue2 ONE = new MolangValue2(MolangValue.ONE, MolangValue.ONE);

    public static final MolangValue2 AXIS_X = new MolangValue2(MolangValue.ONE, MolangValue.ZERO);
    public static final MolangValue2 AXIS_Y = new MolangValue2(MolangValue.ZERO, MolangValue.ONE);

    public static final Codec<MolangValue2> CODEC = MolangCodecs.fixedSizeList(MolangValue.CODEC, 2)
            .xmap(values -> new MolangValue2(values.get(0), values.get(1)), value -> List.of(value.x, value.y));

    public float getX(MolangScope scope) {
        return x.eval(scope);
    }

    public float getY(MolangScope scope) {
        return y.eval(scope);
    }

    public Vector2f eval(MolangScope scope) {
        return new Vector2f(getX(scope), getY(scope));
    }
}
