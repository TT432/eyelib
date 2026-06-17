package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:player.level — 玩家等级。
 *
 * @author TT432
 */
@NullMarked
public record PlayerLevel(
        int value,
        int max
) implements Component {
    public static final Codec<PlayerLevel> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("value", 0).forGetter(PlayerLevel::value),
            Codec.INT.optionalFieldOf("max", 24791).forGetter(PlayerLevel::max)
    ).apply(ins, PlayerLevel::new));

    @Override
    public String id() {
        return "player.level";
    }
}
