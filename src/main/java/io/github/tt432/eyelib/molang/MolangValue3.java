package io.github.tt432.eyelib.molang;

import com.google.gson.JsonArray;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * @author TT432
 */
public record MolangValue3(
        MolangValue x,
        MolangValue y,
        MolangValue z
) {
    public static final Codec<MolangValue3> CODEC = Codec.either(
                    Codec.either(Codec.STRING, Codec.FLOAT)
                            .xmap(e -> e.right().map(Object::toString).orElseGet(() -> e.left().get()), Either::left)
                            .listOf(),
                    RecordCodecBuilder.<MolangValue3>create(ins -> ins.group(
                            MolangValue.CODEC.optionalFieldOf("x", MolangValue.FALSE_VALUE).forGetter(o -> o.x),
                            MolangValue.CODEC.optionalFieldOf("y", MolangValue.FALSE_VALUE).forGetter(o -> o.y),
                            MolangValue.CODEC.optionalFieldOf("z", MolangValue.FALSE_VALUE).forGetter(o -> o.z)
                    ).apply(ins, MolangValue3::new)))
            .xmap(e -> e.left().map(sl -> new MolangValue3(
                                    MolangValue.parse(sl.get(0)),
                                    MolangValue.parse(sl.get(1)),
                                    MolangValue.parse(sl.get(2))))
                            .orElseGet(() -> e.right().get()),
                    m3 -> Either.left(List.of(m3.x.getContext(), m3.y.getContext(), m3.z.getContext())));

    public static final MolangValue3 ZERO = new MolangValue3(MolangValue.FALSE_VALUE, MolangValue.FALSE_VALUE, MolangValue.FALSE_VALUE);

    public static final MolangValue3 AXIS_X = new MolangValue3(MolangValue.TRUE_VALUE, MolangValue.FALSE_VALUE, MolangValue.FALSE_VALUE);
    public static final MolangValue3 AXIS_Y = new MolangValue3(MolangValue.FALSE_VALUE, MolangValue.TRUE_VALUE, MolangValue.FALSE_VALUE);
    public static final MolangValue3 AXIS_Z = new MolangValue3(MolangValue.FALSE_VALUE, MolangValue.FALSE_VALUE, MolangValue.TRUE_VALUE);

    public static MolangValue3 parse(JsonArray jsonArray) {
        return CODEC.parse(JsonOps.INSTANCE, jsonArray)
                .getOrThrow(true, RuntimeException::new);
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
