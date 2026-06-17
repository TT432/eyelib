package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:cannot_be_attacked — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record CannotBeAttacked() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final CannotBeAttacked INSTANCE = new CannotBeAttacked();

    public static final Codec<CannotBeAttacked> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "cannot_be_attacked";
    }
}
