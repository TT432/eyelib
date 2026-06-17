package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;

/**
 * minecraft:physics
 *
 * @param has_gravity               默认 true
 * @param has_collision             默认 true
 * @param push_towards_closest_space 默认 false
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Physics(
        boolean has_gravity,
        boolean has_collision,
        boolean push_towards_closest_space
) implements Component {
    public static final Codec<Physics> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.BOOL.optionalFieldOf("has_gravity", true).forGetter(Physics::has_gravity),
            Codec.BOOL.optionalFieldOf("has_collision", true).forGetter(Physics::has_collision),
            Codec.BOOL.optionalFieldOf("push_towards_closest_space", false).forGetter(Physics::push_towards_closest_space)
    ).apply(ins, Physics::new));

    @Override
    public String id() {
        return "physics";
    }
}
