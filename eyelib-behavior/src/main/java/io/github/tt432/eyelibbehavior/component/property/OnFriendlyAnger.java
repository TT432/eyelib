package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:on_friendly_anger — 友方生物被激怒时触发事件。
 *
 * @author TT432
 */
@NullMarked
public record OnFriendlyAnger(
        String event,
        String target
) implements Component {
    public static final Codec<OnFriendlyAnger> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("event").forGetter(OnFriendlyAnger::event),
            Codec.STRING.optionalFieldOf("target", "self").forGetter(OnFriendlyAnger::target)
    ).apply(ins, OnFriendlyAnger::new));

    @Override
    public String id() {
        return "on_friendly_anger";
    }
}
