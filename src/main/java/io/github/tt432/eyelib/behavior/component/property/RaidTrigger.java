package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;

/**
 * minecraft:raid_trigger
 *
 * @param triggered_event 事件触发配置
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record RaidTrigger(
        TriggeredEvent triggered_event
) implements Component {
    public static final Codec<RaidTrigger> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            TriggeredEvent.CODEC.fieldOf("triggered_event").forGetter(RaidTrigger::triggered_event)
    ).apply(ins, RaidTrigger::new));

    @Override
    public String id() {
        return "raid_trigger";
    }

    public record TriggeredEvent(
            String event,
            String target
    ) {
        public static final Codec<TriggeredEvent> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("event").forGetter(TriggeredEvent::event),
                Codec.STRING.optionalFieldOf("target", "self").forGetter(TriggeredEvent::target)
        ).apply(ins, TriggeredEvent::new));
    }
}
