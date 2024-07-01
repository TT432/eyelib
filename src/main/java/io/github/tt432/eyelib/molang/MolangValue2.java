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
public record MolangValue2(
        MolangValue x,
        MolangValue y
) {
    private static final MolangValue MOLANG0 = MolangValue.FALSE_VALUE;
    private static final MolangValue MOLANG1 = MolangValue.TRUE_VALUE;

    public static final MolangValue2 ZERO = new MolangValue2(MOLANG0, MOLANG0);
    public static final MolangValue2 ONE = new MolangValue2(MOLANG1, MOLANG1);

    public static final MolangValue2 AXIS_X = new MolangValue2(MOLANG1, MOLANG0);
    public static final MolangValue2 AXIS_Y = new MolangValue2(MOLANG0, MOLANG1);

    public static final Codec<MolangValue2> CODEC = Codec.either(
            TupleCodec.tuple(
                    MolangValue.CODEC,
                    MolangValue.CODEC
            ).bmap(MolangValue2::new, mv2 -> Tuple.of(mv2.x, mv2.y)),
            RecordCodecBuilder.<MolangValue2>create(ins -> ins.group(
                    MolangValue.CODEC.optionalFieldOf("x", MOLANG0).forGetter(MolangValue2::x),
                    MolangValue.CODEC.optionalFieldOf("y", MOLANG0).forGetter(MolangValue2::y)
            ).apply(ins, MolangValue2::new))
    ).xmap(Either::unwrap, Either::right);

    public static MolangValue2 parse(JsonElement ele) {
        return CODEC.parse(JsonOps.INSTANCE, ele).getPartialOrThrow();
    }

    public float getX(MolangScope scope) {
        return x.eval(scope);
    }

    public float getY(MolangScope scope) {
        return y.eval(scope);
    }
}
