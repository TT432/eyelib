package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;

/**
 * minecraft:ground_offset
 *
 * @param value 默认 0.0f
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record GroundOffset(
        float value
) implements Component {
    public static final Codec<GroundOffset> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("value", 0.0f).forGetter(GroundOffset::value)
    ).apply(ins, GroundOffset::new));

    @Override
    public String id() {
        return "ground_offset";
    }
}
