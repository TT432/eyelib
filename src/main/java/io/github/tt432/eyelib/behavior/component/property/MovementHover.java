package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:movement.hover — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record MovementHover() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final MovementHover INSTANCE = new MovementHover();

    public static final Codec<MovementHover> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "movement.hover";
    }
}
