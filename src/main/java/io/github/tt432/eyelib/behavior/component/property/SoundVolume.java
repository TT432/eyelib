package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;

/**
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record SoundVolume(
        float value
) implements Component {
    public static final Codec<SoundVolume> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("value", 1.0f).forGetter(SoundVolume::value)
    ).apply(ins, SoundVolume::new));

    @Override
    public String id() {
        return "sound_volume";
    }
}
