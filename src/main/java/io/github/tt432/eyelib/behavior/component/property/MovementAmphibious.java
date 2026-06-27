package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:movement.amphibious — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record MovementAmphibious() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final MovementAmphibious INSTANCE = new MovementAmphibious();

    public static final Codec<MovementAmphibious> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "movement.amphibious";
    }
}
