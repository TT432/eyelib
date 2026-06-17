package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;

/**
 * minecraft:walk_animation_speed
 *
 * @param value 默认 1.0f
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record WalkAnimationSpeed(
        float value
) implements Component {
    public static final Codec<WalkAnimationSpeed> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("value", 1.0f).forGetter(WalkAnimationSpeed::value)
    ).apply(ins, WalkAnimationSpeed::new));

    @Override
    public String id() {
        return "walk_animation_speed";
    }
}
