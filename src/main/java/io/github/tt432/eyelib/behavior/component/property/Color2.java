package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;

/**
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Color2(
        int value
) implements Component {
    public static final Codec<Color2> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("value", 0).forGetter(Color2::value)
    ).apply(ins, Color2::new));

    @Override
    public String id() {
        return "color2";
    }
}
