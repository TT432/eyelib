package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:vibration_listener — 振动监听器（标记组件）。
 *
 * @author TT432
 */
@NullMarked
public record VibrationListener() implements Component {
    public static final VibrationListener INSTANCE = new VibrationListener();

    public static final Codec<VibrationListener> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "vibration_listener";
    }
}
