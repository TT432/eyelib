package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;
import io.github.tt432.eyelib.behavior.component.Component;
/**
 * minecraft:inside_block_notifier — 方块内部通知器（标记组件）。
 *
 * @author TT432
 */
public record InsideBlockNotifier() implements Component {
    public static final InsideBlockNotifier INSTANCE = new InsideBlockNotifier();

    public static final Codec<InsideBlockNotifier> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "inside_block_notifier";
    }
}
