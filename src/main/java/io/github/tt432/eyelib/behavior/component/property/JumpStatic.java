package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;

/**
 * minecraft:jump.static
 *
 * @param jump_power 默认 0.42f
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record JumpStatic(
        float jump_power
) implements Component {
    public static final Codec<JumpStatic> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("jump_power", 0.42f).forGetter(JumpStatic::jump_power)
    ).apply(ins, JumpStatic::new));

    @Override
    public String id() {
        return "jump.static";
    }
}
