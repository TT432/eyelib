package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:rail_movement — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record RailMovement() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final RailMovement INSTANCE = new RailMovement();

    public static final Codec<RailMovement> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "rail_movement";
    }
}
