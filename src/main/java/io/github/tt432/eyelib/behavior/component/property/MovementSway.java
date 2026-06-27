package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:movement.sway — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record MovementSway() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final MovementSway INSTANCE = new MovementSway();

    public static final Codec<MovementSway> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "movement.sway";
    }
}
