package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
/**
 * minecraft:on_hurt_by_player — 实体被玩家伤害时触发事件。
 *
 * @author TT432
 */
public record OnHurtByPlayer(
        String event,
        String target
) implements Component {
    public static final Codec<OnHurtByPlayer> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("event").forGetter(OnHurtByPlayer::event),
            Codec.STRING.optionalFieldOf("target", "self").forGetter(OnHurtByPlayer::target)
    ).apply(ins, OnHurtByPlayer::new));

    @Override
    public String id() {
        return "on_hurt_by_player";
    }
}
