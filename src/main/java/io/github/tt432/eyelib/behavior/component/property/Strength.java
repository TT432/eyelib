package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;

/**
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Strength(
        int value,
        int max
) implements Component {
    public static final Codec<Strength> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("value", 0).forGetter(Strength::value),
            Codec.INT.optionalFieldOf("max", 50).forGetter(Strength::max)
    ).apply(ins, Strength::new));

    @Override
    public String id() {
        return "strength";
    }
}
