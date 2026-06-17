package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * minecraft:knockback_resistance — 实体击退抗性属性。
 * Bedrock 规范: { "value": float, "max": float (optional) }
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record KnockbackResistance(
        float value,
        Optional<Float> max
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<KnockbackResistance> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("value", 0.0f).forGetter(KnockbackResistance::value),
            Codec.FLOAT.optionalFieldOf("max").forGetter(KnockbackResistance::max)
    ).apply(ins, KnockbackResistance::new));

    @Override
    public String id() {
        return "knockback_resistance";
    }
}
