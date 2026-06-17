package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:movement.generic — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record MovementGeneric() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final MovementGeneric INSTANCE = new MovementGeneric();

    public static final Codec<MovementGeneric> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "movement.generic";
    }
}
