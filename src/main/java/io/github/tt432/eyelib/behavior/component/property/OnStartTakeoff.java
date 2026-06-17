package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * minecraft:on_start_takeoff — 实体开始起飞事件触发器。
 * Bedrock 规范: { "event": string, "target": string, "filters": ... }
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record OnStartTakeoff(
        String event,
        String target
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<OnStartTakeoff> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("event").forGetter(OnStartTakeoff::event),
            Codec.STRING.optionalFieldOf("target", "self").forGetter(OnStartTakeoff::target)
    ).apply(ins, OnStartTakeoff::new));

    @Override
    public String id() {
        return "on_start_takeoff";
    }
}
