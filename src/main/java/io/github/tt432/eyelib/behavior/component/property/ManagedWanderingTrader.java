package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:managed_wandering_trader — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record ManagedWanderingTrader() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final ManagedWanderingTrader INSTANCE = new ManagedWanderingTrader();

    public static final Codec<ManagedWanderingTrader> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "managed_wandering_trader";
    }
}
