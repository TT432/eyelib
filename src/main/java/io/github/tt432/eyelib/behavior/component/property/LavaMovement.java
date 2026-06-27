package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:lava_movement — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record LavaMovement() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final LavaMovement INSTANCE = new LavaMovement();

    public static final Codec<LavaMovement> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "lava_movement";
    }
}
