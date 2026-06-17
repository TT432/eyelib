package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:on_wake_with_owner — 实体与主人同时苏醒时触发事件。
 *
 * @author TT432
 */
@NullMarked
public record OnWakeWithOwner(
        String event,
        String target
) implements Component {
    public static final Codec<OnWakeWithOwner> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("event").forGetter(OnWakeWithOwner::event),
            Codec.STRING.optionalFieldOf("target", "self").forGetter(OnWakeWithOwner::target)
    ).apply(ins, OnWakeWithOwner::new));

    @Override
    public String id() {
        return "on_wake_with_owner";
    }
}
