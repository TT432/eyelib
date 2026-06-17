package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * minecraft:leashable
 *
 * @param soft_distance   soft leash distance (default 4.0f)
 * @param hard_distance   hard leash distance (default 6.0f)
 * @param max_distance    max leash distance (default 10.0f)
 * @param on_leash        event when leashed (default target "self")
 * @param on_unleash      event when unleashed (default target "self")
 * @param can_be_stolen   whether can be stolen (default false)
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Leashable(
        float soft_distance,
        float hard_distance,
        float max_distance,
        EventRef on_leash,
        EventRef on_unleash,
        boolean can_be_stolen
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<Leashable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("soft_distance", 4.0f).forGetter(Leashable::soft_distance),
            Codec.FLOAT.optionalFieldOf("hard_distance", 6.0f).forGetter(Leashable::hard_distance),
            Codec.FLOAT.optionalFieldOf("max_distance", 10.0f).forGetter(Leashable::max_distance),
            EventRef.CODEC.optionalFieldOf("on_leash", EventRef.NONE).forGetter(Leashable::on_leash),
            EventRef.CODEC.optionalFieldOf("on_unleash", EventRef.NONE).forGetter(Leashable::on_unleash),
            Codec.BOOL.optionalFieldOf("can_be_stolen", false).forGetter(Leashable::can_be_stolen)
    ).apply(inst, Leashable::new));

    @Override
    public String id() {
        return "leashable";
    }
}
