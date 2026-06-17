package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
/**
 * minecraft:player.saturation — 玩家饱和度。
 *
 * @author TT432
 */
public record PlayerSaturation(
        float value,
        float max
) implements Component {
    public static final Codec<PlayerSaturation> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("value", 5.0f).forGetter(PlayerSaturation::value),
            Codec.FLOAT.optionalFieldOf("max", 20.0f).forGetter(PlayerSaturation::max)
    ).apply(ins, PlayerSaturation::new));

    @Override
    public String id() {
        return "player.saturation";
    }
}
