package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;

/**
 * minecraft:underwater_movement
 *
 * @param value    默认 0.025f
 * @param interval 默认 20.0f
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record UnderwaterMovement(
        float value,
        float interval
) implements Component {
    public static final Codec<UnderwaterMovement> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("value", 0.025f).forGetter(UnderwaterMovement::value),
            Codec.FLOAT.optionalFieldOf("interval", 20.0f).forGetter(UnderwaterMovement::interval)
    ).apply(ins, UnderwaterMovement::new));

    @Override
    public String id() {
        return "underwater_movement";
    }
}
