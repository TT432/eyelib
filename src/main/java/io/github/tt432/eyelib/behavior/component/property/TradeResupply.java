package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:trade_resupply — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record TradeResupply() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final TradeResupply INSTANCE = new TradeResupply();

    public static final Codec<TradeResupply> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "trade_resupply";
    }
}
