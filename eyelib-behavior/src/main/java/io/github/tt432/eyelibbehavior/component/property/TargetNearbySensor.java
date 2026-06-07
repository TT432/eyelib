package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:target_nearby_sensor — 目标距离传感器，检测目标进入/离开范围。
 *
 * @author TT432
 */
@NullMarked
public record TargetNearbySensor(
        float inside_range,
        float outside_range,
        OnRangeEvent on_inside_range,
        OnRangeEvent on_outside_range,
        OnRangeEvent on_vision_lost_inside_range
) implements Component {
    /**
     * 范围事件配置。
     */
    @NullMarked
    public record OnRangeEvent(
            String event,
            String target
    ) {
        public static final Codec<OnRangeEvent> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("event").forGetter(OnRangeEvent::event),
                Codec.STRING.optionalFieldOf("target", "self").forGetter(OnRangeEvent::target)
        ).apply(ins, OnRangeEvent::new));
    }

    public static final Codec<TargetNearbySensor> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("inside_range", 0.0f).forGetter(TargetNearbySensor::inside_range),
            Codec.FLOAT.optionalFieldOf("outside_range", 0.0f).forGetter(TargetNearbySensor::outside_range),
            OnRangeEvent.CODEC.optionalFieldOf("on_inside_range", new OnRangeEvent("", "self")).forGetter(TargetNearbySensor::on_inside_range),
            OnRangeEvent.CODEC.optionalFieldOf("on_outside_range", new OnRangeEvent("", "self")).forGetter(TargetNearbySensor::on_outside_range),
            OnRangeEvent.CODEC.optionalFieldOf("on_vision_lost_inside_range", new OnRangeEvent("", "self")).forGetter(TargetNearbySensor::on_vision_lost_inside_range)
    ).apply(ins, TargetNearbySensor::new));

    @Override
    public String id() {
        return "target_nearby_sensor";
    }
}
