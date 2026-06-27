package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;
import io.github.tt432.eyelib.behavior.component.Component;
/**
 * minecraft:suspect_tracking — 嫌疑追踪（标记组件）。
 *
 * @author TT432
 */
public record SuspectTracking() implements Component {
    public static final SuspectTracking INSTANCE = new SuspectTracking();

    public static final Codec<SuspectTracking> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "suspect_tracking";
    }
}
