package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * minecraft:despawn — 实体消失规则属性。
 * Bedrock 规范: { "despawn_from_distance": DespawnRule (optional) }
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Despawn(
        Optional<Despawn.DespawnRule> despawn_from_distance
) implements io.github.tt432.eyelib.behavior.component.Component {

    /**
     * 距离消失规则。
     *
     * @param min_range          最小范围，默认 32
     * @param max_range          最大范围，默认 128
     * @param despawn_from_chance 消失概率，默认 1
     */
    public record DespawnRule(
            int min_range,
            int max_range,
            int despawn_from_chance
    ) {
        public static final Codec<DespawnRule> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.INT.optionalFieldOf("min_range", 32).forGetter(DespawnRule::min_range),
                Codec.INT.optionalFieldOf("max_range", 128).forGetter(DespawnRule::max_range),
                Codec.INT.optionalFieldOf("despawn_from_chance", 1).forGetter(DespawnRule::despawn_from_chance)
        ).apply(ins, DespawnRule::new));
    }

    public static final Codec<Despawn> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            DespawnRule.CODEC.optionalFieldOf("despawn_from_distance").forGetter(Despawn::despawn_from_distance)
    ).apply(ins, Despawn::new));

    @Override
    public String id() {
        return "despawn";
    }
}
