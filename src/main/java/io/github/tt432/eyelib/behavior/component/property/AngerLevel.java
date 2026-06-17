package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
import java.util.Optional;

/**
 * @author TT432
 */
public record AngerLevel(
        int value,
        Optional<Integer> max,
        int angry_boost,
        float angry_decrement_interval,
        int angry_threshold,
        Optional<Integer> default_annoyingness,
        Optional<Integer> default_projectile_annoyingness,
        Optional<String> on_increase_sounds,
        Optional<String> sounds
) implements Component {
    public static final Codec<AngerLevel> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("value", 0).forGetter(AngerLevel::value),
            Codec.INT.optionalFieldOf("max").forGetter(AngerLevel::max),
            Codec.INT.optionalFieldOf("angry_boost", 20).forGetter(AngerLevel::angry_boost),
            Codec.FLOAT.optionalFieldOf("angry_decrement_interval", 1.0f).forGetter(AngerLevel::angry_decrement_interval),
            Codec.INT.optionalFieldOf("angry_threshold", 80).forGetter(AngerLevel::angry_threshold),
            Codec.INT.optionalFieldOf("default_annoyingness").forGetter(AngerLevel::default_annoyingness),
            Codec.INT.optionalFieldOf("default_projectile_annoyingness").forGetter(AngerLevel::default_projectile_annoyingness),
            Codec.STRING.optionalFieldOf("on_increase_sounds").forGetter(AngerLevel::on_increase_sounds),
            Codec.STRING.optionalFieldOf("sounds").forGetter(AngerLevel::sounds)
    ).apply(ins, AngerLevel::new));

    @Override
    public String id() {
        return "anger_level";
    }
}
