package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;

import java.util.List;

/**
 * minecraft:celebrate_hunt
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record CelebrateHunt(
        List<String> celebration_targets,
        boolean celebrate,
        boolean broadcast,
        int duration,
        int radius,
        int sound_interval,
        int interval
) implements Component {
    public static final Codec<CelebrateHunt> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.listOf().optionalFieldOf("celebration_targets", List.of()).forGetter(CelebrateHunt::celebration_targets),
            Codec.BOOL.optionalFieldOf("celebrate", true).forGetter(CelebrateHunt::celebrate),
            Codec.BOOL.optionalFieldOf("broadcast", true).forGetter(CelebrateHunt::broadcast),
            Codec.INT.optionalFieldOf("duration", 4).forGetter(CelebrateHunt::duration),
            Codec.INT.optionalFieldOf("radius", 16).forGetter(CelebrateHunt::radius),
            Codec.INT.optionalFieldOf("sound_interval", 0).forGetter(CelebrateHunt::sound_interval),
            Codec.INT.optionalFieldOf("interval", 1).forGetter(CelebrateHunt::interval)
    ).apply(ins, CelebrateHunt::new));

    @Override
    public String id() {
        return "celebrate_hunt";
    }
}
