package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;

/**
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record FlyingSpeed(
        float value
) implements Component {
    public static final Codec<FlyingSpeed> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("value", 0.4f).forGetter(FlyingSpeed::value)
    ).apply(ins, FlyingSpeed::new));

    @Override
    public String id() {
        return "flying_speed";
    }
}
