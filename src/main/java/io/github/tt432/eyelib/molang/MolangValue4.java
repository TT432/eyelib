package io.github.tt432.eyelib.molang;

import com.mojang.serialization.Codec;
import io.github.tt432.chin.codec.ChinExtraCodecs;
import io.github.tt432.chin.util.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector4f;

/**
 * @author TT432
 */
@Slf4j
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

    public static final Codec<MolangValue4> CODEC = ChinExtraCodecs
            .tuple(MolangValue.CODEC, MolangValue.CODEC, MolangValue.CODEC, MolangValue.CODEC)
            .bmap(MolangValue4::new, mv4 -> Tuple.of(mv4.x, mv4.y, mv4.z, mv4.w));

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
