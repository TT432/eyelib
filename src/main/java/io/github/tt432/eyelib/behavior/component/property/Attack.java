package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
/**
 * @author TT432
 */
public record Attack(
        String damage,
        float effect_duration,
        String effect_name,
        int effect_amplifier
) implements Component {
    public static final Codec<Attack> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.either(Codec.INT, Codec.STRING).xmap(
                    r -> r.map(i -> Integer.toString(i), s -> s),
                    s -> com.mojang.datafixers.util.Either.right(s)
            ).fieldOf("damage").forGetter(Attack::damage),
            Codec.FLOAT.optionalFieldOf("effect_duration", 0f).forGetter(Attack::effect_duration),
            Codec.STRING.optionalFieldOf("effect_name", "").forGetter(Attack::effect_name),
            Codec.INT.optionalFieldOf("effect_amplifier", 0).forGetter(Attack::effect_amplifier)
    ).apply(ins, Attack::new));

    @Override
    public String id() {
        return "attack";
    }
}
