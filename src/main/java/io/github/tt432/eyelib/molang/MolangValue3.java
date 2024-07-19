package io.github.tt432.eyelib.molang;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.Tuple;
import io.github.tt432.eyelib.util.codec.TupleCodec;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3f;

/**
 * @author TT432
 */
@Slf4j
public record MolangValue3(
        MolangValue x,
        MolangValue y,
        MolangValue z
) {
    public static final MolangValue3 ZERO = new MolangValue3(MolangValue.ZERO, MolangValue.ZERO, MolangValue.ZERO);

    public static final MolangValue3 AXIS_X = new MolangValue3(MolangValue.ONE, MolangValue.ZERO, MolangValue.ZERO);
    public static final MolangValue3 AXIS_Y = new MolangValue3(MolangValue.ZERO, MolangValue.ONE, MolangValue.ZERO);
    public static final MolangValue3 AXIS_Z = new MolangValue3(MolangValue.ZERO, MolangValue.ZERO, MolangValue.ONE);

    public static final Codec<MolangValue3> CODEC = TupleCodec.tuple(MolangValue.CODEC, MolangValue.CODEC, MolangValue.CODEC)
            .bmap(MolangValue3::new, mv3 -> Tuple.of(mv3.x, mv3.y, mv3.z));

    public float getX(MolangScope scope) {
        return x.eval(scope);
    }

    public float getY(MolangScope scope) {
        return y.eval(scope);
    }

    public float getZ(MolangScope scope) {
        return z.eval(scope);
    }

    public Vector3f eval(MolangScope scope) {
        return new Vector3f(getX(scope), getY(scope), getZ(scope));
    }
}
