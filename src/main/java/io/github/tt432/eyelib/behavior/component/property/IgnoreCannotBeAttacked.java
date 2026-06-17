package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:ignore_cannot_be_attacked — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IgnoreCannotBeAttacked() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final IgnoreCannotBeAttacked INSTANCE = new IgnoreCannotBeAttacked();

    public static final Codec<IgnoreCannotBeAttacked> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "ignore_cannot_be_attacked";
    }
}
