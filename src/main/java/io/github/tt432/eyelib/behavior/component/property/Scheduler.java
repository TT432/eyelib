package io.github.tt432.eyelib.behavior.component.property;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * minecraft:scheduler — 调度器组件，在一段时间后安排事件。
 *
 * @author TT432
 */
@NullMarked
public record Scheduler(
        float min_delay_secs,
        float max_delay_secs,
        List<ScheduledEvent> scheduled_events
) implements Component {
    private static final Codec<JsonObject> JSON_OBJECT_CODEC = Codec.STRING.xmap(
            s -> JsonParser.parseString(s).getAsJsonObject(),
            Object::toString
    );

    /**
     * 调度事件配置。
     */
    @NullMarked
    public record ScheduledEvent(
            JsonObject filters,
            String event
    ) {
        public static final Codec<ScheduledEvent> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                JSON_OBJECT_CODEC.optionalFieldOf("filters", new com.google.gson.JsonObject()).forGetter(ScheduledEvent::filters),
                Codec.STRING.fieldOf("event").forGetter(ScheduledEvent::event)
        ).apply(ins, ScheduledEvent::new));
    }

    public static final Codec<Scheduler> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.fieldOf("min_delay_secs").forGetter(Scheduler::min_delay_secs),
            Codec.FLOAT.fieldOf("max_delay_secs").forGetter(Scheduler::max_delay_secs),
            ScheduledEvent.CODEC.listOf().fieldOf("scheduled_events").forGetter(Scheduler::scheduled_events)
    ).apply(ins, Scheduler::new));

    @Override
    public String id() {
        return "scheduler";
    }
}
