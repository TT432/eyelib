package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:trade_resupply — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record TradeResupply() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final TradeResupply INSTANCE = new TradeResupply();

    public static final Codec<TradeResupply> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "trade_resupply";
    }
}
