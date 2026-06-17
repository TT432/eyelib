package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;

import java.util.List;

/**
 * minecraft:movement.jump
 *
 * @param jump_delay 默认 [0.0f, 0.0f]
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record MovementJump(
        List<Float> jump_delay
) implements Component {
    public static final Codec<MovementJump> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.listOf().optionalFieldOf("jump_delay", List.of(0.0f, 0.0f)).forGetter(MovementJump::jump_delay)
    ).apply(ins, MovementJump::new));

    @Override
    public String id() {
        return "movement.jump";
    }
}
