package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:movement.hover — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record MovementHover() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final MovementHover INSTANCE = new MovementHover();

    public static final Codec<MovementHover> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "movement.hover";
    }
}
