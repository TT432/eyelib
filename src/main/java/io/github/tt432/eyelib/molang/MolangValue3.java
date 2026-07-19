package io.github.tt432.eyelib.molang;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author TT432
 */
public record MolangValue3(
        MolangValue x,
        MolangValue y,
        MolangValue z
) {
    public static final MolangValue3 ZERO = new MolangValue3(MolangValue.ZERO, MolangValue.ZERO, MolangValue.ZERO);

    public static final MolangValue3 AXIS_X = new MolangValue3(MolangValue.ONE, MolangValue.ZERO, MolangValue.ZERO);
    public static final MolangValue3 AXIS_Y = new MolangValue3(MolangValue.ZERO, MolangValue.ONE, MolangValue.ZERO);
    public static final MolangValue3 AXIS_Z = new MolangValue3(MolangValue.ZERO, MolangValue.ZERO, MolangValue.ONE);

    private static final Codec<MolangValue3> LIST_CODEC = MolangCodecs.fixedSizeList(MolangValue.CODEC, 3)
            .xmap(values -> new MolangValue3(values.get(0), values.get(1), values.get(2)), value -> List.of(value.x, value.y, value.z));

    private static final Codec<Map.Entry<String, MolangValue>> AXIS_ENTRY_CODEC = Codec.either(entry("x"), Codec.either(entry("y"), entry("z")))
            .xmap(either -> either.map(Function.identity(), nested -> nested.map(Function.identity(), Function.identity())), Either::left);

    public static final Codec<MolangValue3> CODEC = Codec.either(LIST_CODEC, AXIS_ENTRY_CODEC.listOf())
            .xmap(either -> either.map(Function.identity(), MolangValue3::fromEntries), Either::left);

    static Codec<Map.Entry<String, MolangValue>> entry(String axis) {
        return MolangValue.CODEC.fieldOf(axis).codec().flatXmap(
                value -> DataResult.success(Map.entry(axis, value)),
                entry -> axis.equals(entry.getKey())
                        ? DataResult.success(entry.getValue())
                        : DataResult.error(() -> "can't parse other entry in " + axis)
        );
    }

    private static MolangValue3 fromEntries(List<Map.Entry<String, MolangValue>> entries) {
        Map<String, MolangValue> values = new HashMap<>();
        for (Map.Entry<String, MolangValue> entry : entries) {
            values.put(entry.getKey(), entry.getValue());
        }

        return new MolangValue3(
                values.getOrDefault("x", MolangValue.ZERO),
                values.getOrDefault("y", MolangValue.ZERO),
                values.getOrDefault("z", MolangValue.ZERO)
        );
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

    /**
     * 逐轴绑定 molang `this` 后求值。`this` = 表达式最终写入目标的当前值
     * （Mojang molang syntax-guide），在关键帧语境下为该骨骼该通道的当前值。
     */
    public Vector3f evalWithThis(MolangScope scope, float thisX, float thisY, float thisZ) {
        scope.set("this", thisX);
        float x = getX(scope);
        scope.set("this", thisY);
        float y = getY(scope);
        scope.set("this", thisZ);
        float z = getZ(scope);
        return new Vector3f(x, y, z);
    }
}