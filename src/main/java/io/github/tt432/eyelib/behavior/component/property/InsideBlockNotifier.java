package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:inside_block_notifier — 方块内部通知器（标记组件）。
 *
 * @author TT432
 */
@NullMarked
public record InsideBlockNotifier() implements Component {
    public static final InsideBlockNotifier INSTANCE = new InsideBlockNotifier();

    public static final Codec<InsideBlockNotifier> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "inside_block_notifier";
    }
}
