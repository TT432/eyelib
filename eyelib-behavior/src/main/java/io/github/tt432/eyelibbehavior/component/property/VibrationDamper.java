package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:vibration_damper — 振动阻尼器（标记组件）。
 *
 * @author TT432
 */
@NullMarked
public record VibrationDamper() implements Component {
    public static final VibrationDamper INSTANCE = new VibrationDamper();

    public static final Codec<VibrationDamper> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "vibration_damper";
    }
}
