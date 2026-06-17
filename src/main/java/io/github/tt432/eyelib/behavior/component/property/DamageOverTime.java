package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;

/**
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record DamageOverTime(
        int damage_per_hurt,
        float time_between_hurt
) implements Component {
    public static final Codec<DamageOverTime> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("damage_per_hurt", 1).forGetter(DamageOverTime::damage_per_hurt),
            Codec.FLOAT.optionalFieldOf("time_between_hurt", 0.0f).forGetter(DamageOverTime::time_between_hurt)
    ).apply(ins, DamageOverTime::new));

    @Override
    public String id() {
        return "damage_over_time";
    }
}
