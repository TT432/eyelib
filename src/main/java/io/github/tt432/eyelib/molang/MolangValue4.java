package io.github.tt432.eyelib.molang;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.codec.Tuple;
import io.github.tt432.eyelib.util.codec.TupleCodec;
import lombok.extern.slf4j.Slf4j;

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
    private static final MolangValue MOLANG0 = MolangValue.FALSE_VALUE;
    private static final MolangValue MOLANG1 = MolangValue.TRUE_VALUE;

    public static final MolangValue4 ZERO = new MolangValue4(MOLANG0, MOLANG0, MOLANG0, MOLANG0);

    public static final MolangValue4 AXIS_X = new MolangValue4(MOLANG1, MOLANG0, MOLANG0, MOLANG0);
    public static final MolangValue4 AXIS_Y = new MolangValue4(MOLANG0, MOLANG1, MOLANG0, MOLANG0);
    public static final MolangValue4 AXIS_Z = new MolangValue4(MOLANG0, MOLANG0, MOLANG1, MOLANG0);
    public static final MolangValue4 AXIS_W = new MolangValue4(MOLANG0, MOLANG0, MOLANG0, MOLANG1);

    public static final Codec<MolangValue4> CODEC = Codec.either(
            TupleCodec.tuple(
                    MolangValue.CODEC,
                    MolangValue.CODEC,
                    MolangValue.CODEC,
                    MolangValue.CODEC
            ).bmap(MolangValue4::new, mv3 -> Tuple.of(mv3.x, mv3.y, mv3.z, mv3.w)),
            RecordCodecBuilder.<MolangValue4>create(ins -> ins.group(
                    MolangValue.CODEC.optionalFieldOf("x", MOLANG0).forGetter(MolangValue4::x),
                    MolangValue.CODEC.optionalFieldOf("y", MOLANG0).forGetter(MolangValue4::y),
                    MolangValue.CODEC.optionalFieldOf("z", MOLANG0).forGetter(MolangValue4::z),
                    MolangValue.CODEC.optionalFieldOf("w", MOLANG0).forGetter(MolangValue4::w)
            ).apply(ins, MolangValue4::new))
    ).xmap(Either::unwrap, Either::right);

    public static MolangValue4 parse(JsonElement ele) {
        return CODEC.parse(JsonOps.INSTANCE, ele).getPartialOrThrow();
    }

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
}
