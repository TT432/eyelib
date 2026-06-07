package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * minecraft:timer — 定时器组件，周期性地触发事件。
 *
 * @author TT432
 */
@NullMarked
public record Timer(
        boolean looping,
        boolean randomInterval,
        List<Float> time,
        TimeDownEvent time_down_event
) implements Component {
    /**
     * 倒计时事件配置。
     */
    @NullMarked
    public record TimeDownEvent(
            String event,
            String target
    ) {
        public static final Codec<TimeDownEvent> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("event").forGetter(TimeDownEvent::event),
                Codec.STRING.optionalFieldOf("target", "self").forGetter(TimeDownEvent::target)
        ).apply(ins, TimeDownEvent::new));
    }

    public static final Codec<Timer> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.BOOL.optionalFieldOf("looping", true).forGetter(Timer::looping),
            Codec.BOOL.optionalFieldOf("randomInterval", false).forGetter(Timer::randomInterval),
            Codec.FLOAT.listOf().optionalFieldOf("time", List.of(0.0f, 0.0f)).forGetter(Timer::time),
            TimeDownEvent.CODEC.fieldOf("time_down_event").forGetter(Timer::time_down_event)
    ).apply(ins, Timer::new));

    @Override
    public String id() {
        return "timer";
    }
}
