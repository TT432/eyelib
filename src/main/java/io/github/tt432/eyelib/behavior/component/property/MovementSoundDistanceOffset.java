package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;

/**
 * minecraft:movement_sound_distance_offset
 *
 * @param value 默认 0.0f
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record MovementSoundDistanceOffset(
        float value
) implements Component {
    public static final Codec<MovementSoundDistanceOffset> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("value", 0.0f).forGetter(MovementSoundDistanceOffset::value)
    ).apply(ins, MovementSoundDistanceOffset::new));

    @Override
    public String id() {
        return "movement_sound_distance_offset";
    }
}
