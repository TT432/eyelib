package io.github.tt432.eyelib.molang;

import com.mojang.serialization.Codec;
import io.github.tt432.chin.codec.ChinExtraCodecs;
import io.github.tt432.chin.util.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2f;

/**
 * @author TT432
 */
@Slf4j
public record MolangValue2(
        MolangValue x,
        MolangValue y
) {
    public static final MolangValue2 ZERO = new MolangValue2(MolangValue.ZERO, MolangValue.ZERO);
    public static final MolangValue2 ONE = new MolangValue2(MolangValue.ONE, MolangValue.ONE);

    public static final MolangValue2 AXIS_X = new MolangValue2(MolangValue.ONE, MolangValue.ZERO);
    public static final MolangValue2 AXIS_Y = new MolangValue2(MolangValue.ZERO, MolangValue.ONE);

    public static final Codec<MolangValue2> CODEC = ChinExtraCodecs.tuple(MolangValue.CODEC, MolangValue.CODEC)
            .bmap(MolangValue2::new, mv2 -> Tuple.of(mv2.x, mv2.y));

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
