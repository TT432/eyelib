package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
/**
 * minecraft:tick_world — 世界刻处理组件，控制实体所在区块的加载。
 *
 * @author TT432
 */
public record TickWorld(
        int radius,
        int distance_to_players,
        boolean never_despawn
) implements Component {
    public static final Codec<TickWorld> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("radius", 2).forGetter(TickWorld::radius),
            Codec.INT.optionalFieldOf("distance_to_players", 128).forGetter(TickWorld::distance_to_players),
            Codec.BOOL.optionalFieldOf("never_despawn", true).forGetter(TickWorld::never_despawn)
    ).apply(ins, TickWorld::new));

    @Override
    public String id() {
        return "tick_world";
    }
}
