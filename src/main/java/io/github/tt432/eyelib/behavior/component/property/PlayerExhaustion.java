package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:player.exhaustion — 玩家饥饿值消耗程度。
 *
 * @author TT432
 */
@NullMarked
public record PlayerExhaustion(
        float value,
        float max
) implements Component {
    public static final Codec<PlayerExhaustion> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("value", 0.0f).forGetter(PlayerExhaustion::value),
            Codec.FLOAT.optionalFieldOf("max", 5.0f).forGetter(PlayerExhaustion::max)
    ).apply(ins, PlayerExhaustion::new));

    @Override
    public String id() {
        return "player.exhaustion";
    }
}
