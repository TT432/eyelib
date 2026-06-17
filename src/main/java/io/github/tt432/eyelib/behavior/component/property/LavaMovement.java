package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:lava_movement — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record LavaMovement() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final LavaMovement INSTANCE = new LavaMovement();

    public static final Codec<LavaMovement> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "lava_movement";
    }
}
