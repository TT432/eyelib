package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;

/**
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Color(
        int value
) implements Component {
    public static final Codec<Color> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("value", 0).forGetter(Color::value)
    ).apply(ins, Color::new));

    @Override
    public String id() {
        return "color";
    }
}
