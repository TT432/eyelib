package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * minecraft:movement — 实体移动速度属性。
 * Bedrock 规范: { "value": float, "max": float (optional) }
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Movement(
        float value,
        Optional<Float> max
) implements io.github.tt432.eyelibbehavior.component.Component {
    public static final Codec<Movement> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("value", 0.25f).forGetter(Movement::value),
            Codec.FLOAT.optionalFieldOf("max").forGetter(Movement::max)
    ).apply(ins, Movement::new));

    @Override
    public String id() {
        return "movement";
    }
}
