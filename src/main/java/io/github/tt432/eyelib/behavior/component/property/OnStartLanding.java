package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * minecraft:on_start_landing — 实体开始着陆事件触发器。
 * Bedrock 规范: { "event": string, "target": string }
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record OnStartLanding(
        String event,
        String target
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<OnStartLanding> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("event").forGetter(OnStartLanding::event),
            Codec.STRING.optionalFieldOf("target", "self").forGetter(OnStartLanding::target)
    ).apply(ins, OnStartLanding::new));

    @Override
    public String id() {
        return "on_start_landing";
    }
}
