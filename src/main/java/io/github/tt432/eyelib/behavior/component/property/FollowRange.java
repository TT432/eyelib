package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * minecraft:follow_range — 实体跟随范围属性。
 * Bedrock 规范: { "value": int, "max": int (optional) }
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record FollowRange(
        int value,
        Optional<Integer> max
) implements io.github.tt432.eyelibbehavior.component.Component {
    public static final Codec<FollowRange> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("value", 16).forGetter(FollowRange::value),
            Codec.INT.optionalFieldOf("max").forGetter(FollowRange::max)
    ).apply(ins, FollowRange::new));

    @Override
    public String id() {
        return "follow_range";
    }
}
