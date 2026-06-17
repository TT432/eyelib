package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
/**
 * minecraft:player.experience — 玩家经验值。
 *
 * @author TT432
 */
public record PlayerExperience(
        int value,
        int max
) implements Component {
    public static final Codec<PlayerExperience> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("value", 0).forGetter(PlayerExperience::value),
            Codec.INT.optionalFieldOf("max", 2147483647).forGetter(PlayerExperience::max)
    ).apply(ins, PlayerExperience::new));

    @Override
    public String id() {
        return "player.experience";
    }
}
