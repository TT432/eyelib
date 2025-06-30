package io.github.tt432.eyelib.molang;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.tt432.chin.codec.ChinExtraCodecs;
import io.github.tt432.chin.util.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static final Codec<MolangValue3> CODEC = Codec.withAlternative(
            ChinExtraCodecs.tuple(MolangValue.CODEC, MolangValue.CODEC, MolangValue.CODEC)
                    .bmap(MolangValue3::new, mv3 -> Tuple.of(mv3.x, mv3.y, mv3.z)),
            Codec.withAlternative(Codec.withAlternative(entry("x"), entry("y")), entry("z")).listOf()
                    .xmap(l -> {
                                Map<Object, Object> map = new HashMap<>();
                                for (Map.Entry<String, MolangValue> stringMolangValueEntry : l) {
                                    map.put(stringMolangValueEntry.getKey(), stringMolangValueEntry.getValue());
                                }
                                return new MolangValue3(
                                        (MolangValue) map.getOrDefault("x", MolangValue.ZERO),
                                        (MolangValue) map.getOrDefault("y", MolangValue.ZERO),
                                        (MolangValue) map.getOrDefault("z", MolangValue.ZERO)
                                );
                            },
                            m3 -> List.of(
                                    Map.entry("x", m3.x),
                                    Map.entry("y", m3.y),
                                    Map.entry("z", m3.z)
                            )
                    )
    );

    static Codec<Map.Entry<String, MolangValue>> entry(String axis) {
        return MolangValue.CODEC.fieldOf(axis).codec().flatXmap(m -> DataResult.success(Map.entry(axis, m)), e -> e.getKey().equals(axis) ? DataResult.success(e.getValue()) : DataResult.error(() -> "can't parse other entry in " + axis));
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

    public Vector3f eval(MolangScope scope) {
        return new Vector3f(getX(scope), getY(scope), getZ(scope));
    }
}
