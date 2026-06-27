package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:movement.generic — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record MovementGeneric() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final MovementGeneric INSTANCE = new MovementGeneric();

    public static final Codec<MovementGeneric> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "movement.generic";
    }
}
