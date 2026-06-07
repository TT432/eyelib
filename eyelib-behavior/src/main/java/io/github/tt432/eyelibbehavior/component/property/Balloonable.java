package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * minecraft:balloonable
 *
 * @param soft_distance soft balloon distance (default 2.0f)
 * @param max_distance  max balloon distance (default 10.0f)
 * @param on_balloon    event when ballooned (default target "self")
 * @param on_unballoon  event when unballooned (default target "self")
 * @param mass          mass (default 1.0f)
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Balloonable(
        float soft_distance,
        float max_distance,
        EventRef on_balloon,
        EventRef on_unballoon,
        float mass
) implements io.github.tt432.eyelibbehavior.component.Component {
    public static final Codec<Balloonable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("soft_distance", 2.0f).forGetter(Balloonable::soft_distance),
            Codec.FLOAT.optionalFieldOf("max_distance", 10.0f).forGetter(Balloonable::max_distance),
            EventRef.CODEC.optionalFieldOf("on_balloon", EventRef.NONE).forGetter(Balloonable::on_balloon),
            EventRef.CODEC.optionalFieldOf("on_unballoon", EventRef.NONE).forGetter(Balloonable::on_unballoon),
            Codec.FLOAT.optionalFieldOf("mass", 1.0f).forGetter(Balloonable::mass)
    ).apply(inst, Balloonable::new));

    @Override
    public String id() {
        return "balloonable";
    }
}
