package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:movement.skip — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record MovementSkip() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final MovementSkip INSTANCE = new MovementSkip();

    public static final Codec<MovementSkip> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "movement.skip";
    }
}
