package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:on_hurt — 实体受到伤害时触发事件。
 *
 * @author TT432
 */
@NullMarked
public record OnHurt(
        String event,
        String target
) implements Component {
    public static final Codec<OnHurt> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("event").forGetter(OnHurt::event),
            Codec.STRING.optionalFieldOf("target", "self").forGetter(OnHurt::target)
    ).apply(ins, OnHurt::new));

    @Override
    public String id() {
        return "on_hurt";
    }
}
