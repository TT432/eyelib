package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;

/**
 * @author TT432
 */
@NullMarked
public record AmbientSoundInterval(
        float value,
        Optional<String> event_name,
        Optional<Float> range
) implements Component {
    public static final Codec<AmbientSoundInterval> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("value", 8.0f).forGetter(AmbientSoundInterval::value),
            Codec.STRING.optionalFieldOf("event_name").forGetter(AmbientSoundInterval::event_name),
            Codec.FLOAT.optionalFieldOf("range").forGetter(AmbientSoundInterval::range)
    ).apply(ins, AmbientSoundInterval::new));

    @Override
    public String id() {
        return "ambient_sound_interval";
    }
}
