package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.behavior.component.Component;
/**
 * minecraft:vibration_listener — 振动监听器（标记组件）。
 *
 * @author TT432
 */
public record VibrationListener() implements Component {
    public static final VibrationListener INSTANCE = new VibrationListener();

    public static final Codec<VibrationListener> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "vibration_listener";
    }
}
