package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;
import io.github.tt432.eyelib.behavior.component.Component;
/**
 * minecraft:vibration_damper — 振动阻尼器（标记组件）。
 *
 * @author TT432
 */
public record VibrationDamper() implements Component {
    public static final VibrationDamper INSTANCE = new VibrationDamper();

    public static final Codec<VibrationDamper> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "vibration_damper";
    }
}
