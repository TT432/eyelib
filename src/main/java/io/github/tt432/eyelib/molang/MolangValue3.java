package io.github.tt432.eyelib.molang;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.codec.Codecs;
import io.github.tt432.eyelib.util.codec.Tuple;
import io.github.tt432.eyelib.util.codec.TupleCodec;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TT432
 */
@Slf4j
public record MolangValue3(
        MolangValue x,
        MolangValue y,
        MolangValue z
) {
    private static final MolangValue MOLANG0 = MolangValue.FALSE_VALUE;
    private static final MolangValue MOLANG1 = MolangValue.TRUE_VALUE;

    public static final MolangValue3 ZERO = new MolangValue3(MOLANG0, MOLANG0, MOLANG0);

    public static final MolangValue3 AXIS_X = new MolangValue3(MOLANG1, MOLANG0, MOLANG0);
    public static final MolangValue3 AXIS_Y = new MolangValue3(MOLANG0, MOLANG1, MOLANG0);
    public static final MolangValue3 AXIS_Z = new MolangValue3(MOLANG0, MOLANG0, MOLANG1);

    public static final Codec<MolangValue3> CODEC = Codec.either(
            TupleCodec.tuple(
                    MolangValue.CODEC,
                    MolangValue.CODEC,
                    MolangValue.CODEC
            ).bmap(MolangValue3::new, mv3 -> Tuple.of(mv3.x, mv3.y, mv3.z)),
            RecordCodecBuilder.<MolangValue3>create(ins -> ins.group(
                    MolangValue.CODEC.optionalFieldOf("x", MOLANG0).forGetter(MolangValue3::x),
                    MolangValue.CODEC.optionalFieldOf("y", MOLANG0).forGetter(MolangValue3::y),
                    MolangValue.CODEC.optionalFieldOf("z", MOLANG0).forGetter(MolangValue3::z)
            ).apply(ins, MolangValue3::new))
    ).xmap(Codecs::unwrap, Either::right);

    public static MolangValue3 parse(JsonElement ele) {
        return CODEC.parse(JsonOps.INSTANCE, ele).getOrThrow(true, log::error);
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
}
