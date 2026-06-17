package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
/**
 * minecraft:on_target_acquired — 实体获得目标时触发事件。
 *
 * @author TT432
 */
public record OnTargetAcquired(
        String event,
        String target
) implements Component {
    public static final Codec<OnTargetAcquired> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("event").forGetter(OnTargetAcquired::event),
            Codec.STRING.optionalFieldOf("target", "self").forGetter(OnTargetAcquired::target)
    ).apply(ins, OnTargetAcquired::new));

    @Override
    public String id() {
        return "on_target_acquired";
    }
}
