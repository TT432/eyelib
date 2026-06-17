package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:floats_in_liquid — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record FloatsInLiquid() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final FloatsInLiquid INSTANCE = new FloatsInLiquid();

    public static final Codec<FloatsInLiquid> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "floats_in_liquid";
    }
}
