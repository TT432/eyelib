package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:drying_out_timer — 脱水计时器组件。
 *
 * @author TT432
 */
@NullMarked
public record DryingOutTimer(
        float total_time,
        int water_touch_distance_threshold
) implements Component {
    public static final Codec<DryingOutTimer> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("total_time", 0.0f).forGetter(DryingOutTimer::total_time),
            Codec.INT.optionalFieldOf("water_touch_distance_threshold", 1).forGetter(DryingOutTimer::water_touch_distance_threshold)
    ).apply(ins, DryingOutTimer::new));

    @Override
    public String id() {
        return "drying_out_timer";
    }
}
