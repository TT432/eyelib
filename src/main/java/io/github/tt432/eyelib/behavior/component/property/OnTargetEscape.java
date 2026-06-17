package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
/**
 * minecraft:on_target_escape — 目标逃离时触发事件。
 *
 * @author TT432
 */
public record OnTargetEscape(
        String event,
        String target
) implements Component {
    public static final Codec<OnTargetEscape> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("event").forGetter(OnTargetEscape::event),
            Codec.STRING.optionalFieldOf("target", "self").forGetter(OnTargetEscape::target)
    ).apply(ins, OnTargetEscape::new));

    @Override
    public String id() {
        return "on_target_escape";
    }
}
