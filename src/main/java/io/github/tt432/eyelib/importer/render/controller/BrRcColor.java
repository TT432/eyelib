package io.github.tt432.eyelibimporter.render.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import org.jspecify.annotations.NullMarked;

/**
 * Bedrock render controller 的 color 字段（RGBA Molang 表达式）。
 * 求值后作为 vertex color 应用到渲染管线。
 *
 * @author TT432
 */
@NullMarked
public record BrRcColor(
        MolangValue r,
        MolangValue g,
        MolangValue b,
        MolangValue a
) {
    public static final Codec<BrRcColor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MolangValue.CODEC.optionalFieldOf("r", MolangValue.ONE).forGetter(BrRcColor::r),
            MolangValue.CODEC.optionalFieldOf("g", MolangValue.ONE).forGetter(BrRcColor::g),
            MolangValue.CODEC.optionalFieldOf("b", MolangValue.ONE).forGetter(BrRcColor::b),
            MolangValue.CODEC.optionalFieldOf("a", MolangValue.ONE).forGetter(BrRcColor::a)
    ).apply(instance, BrRcColor::new));

    public float[] eval(MolangScope scope) {
        return new float[]{
                r.eval(scope),
                g.eval(scope),
                b.eval(scope),
                a.eval(scope)
        };
    }
}
