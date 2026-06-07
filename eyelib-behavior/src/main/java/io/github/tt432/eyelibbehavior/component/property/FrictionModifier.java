package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;

/**
 * minecraft:friction_modifier
 *
 * @param value 默认 1.0f
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record FrictionModifier(
        float value
) implements Component {
    public static final Codec<FrictionModifier> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("value", 1.0f).forGetter(FrictionModifier::value)
    ).apply(ins, FrictionModifier::new));

    @Override
    public String id() {
        return "friction_modifier";
    }
}
