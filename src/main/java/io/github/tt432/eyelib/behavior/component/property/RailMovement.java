package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:rail_movement — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record RailMovement() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final RailMovement INSTANCE = new RailMovement();

    public static final Codec<RailMovement> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "rail_movement";
    }
}
