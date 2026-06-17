package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:suspect_tracking — 嫌疑追踪（标记组件）。
 *
 * @author TT432
 */
@NullMarked
public record SuspectTracking() implements Component {
    public static final SuspectTracking INSTANCE = new SuspectTracking();

    public static final Codec<SuspectTracking> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "suspect_tracking";
    }
}
