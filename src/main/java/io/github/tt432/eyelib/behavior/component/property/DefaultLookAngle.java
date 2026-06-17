package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;

/**
 * minecraft:default_look_angle
 *
 * @param value 默认 0.0f
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record DefaultLookAngle(
        float value
) implements Component {
    public static final Codec<DefaultLookAngle> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("value", 0.0f).forGetter(DefaultLookAngle::value)
    ).apply(ins, DefaultLookAngle::new));

    @Override
    public String id() {
        return "default_look_angle";
    }
}
