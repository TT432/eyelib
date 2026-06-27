package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:water_movement — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record WaterMovement() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final WaterMovement INSTANCE = new WaterMovement();

    public static final Codec<WaterMovement> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "water_movement";
    }
}
