package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;

/**
 * minecraft:movement.glide
 *
 * @param start_speed        默认 1.0f
 * @param speed_when_turning 默认 2.0f
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record MovementGlide(
        float start_speed,
        float speed_when_turning
) implements Component {
    public static final Codec<MovementGlide> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("start_speed", 1.0f).forGetter(MovementGlide::start_speed),
            Codec.FLOAT.optionalFieldOf("speed_when_turning", 2.0f).forGetter(MovementGlide::speed_when_turning)
    ).apply(ins, MovementGlide::new));

    @Override
    public String id() {
        return "movement.glide";
    }
}
