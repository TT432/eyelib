package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;

/**
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Absorption(
        int value
) implements Component {
    public static final Codec<Absorption> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("value", 0).forGetter(Absorption::value)
    ).apply(ins, Absorption::new));

    @Override
    public String id() {
        return "absorption";
    }
}
