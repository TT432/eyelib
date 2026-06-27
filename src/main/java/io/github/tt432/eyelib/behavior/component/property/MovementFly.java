package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:movement.fly — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record MovementFly() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final MovementFly INSTANCE = new MovementFly();

    public static final Codec<MovementFly> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "movement.fly";
    }
}
