package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:movement.dolphin — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record MovementDolphin() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final MovementDolphin INSTANCE = new MovementDolphin();

    public static final Codec<MovementDolphin> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "movement.dolphin";
    }
}
