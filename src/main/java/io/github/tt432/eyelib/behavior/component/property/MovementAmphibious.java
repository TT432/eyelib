package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:movement.amphibious — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record MovementAmphibious() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final MovementAmphibious INSTANCE = new MovementAmphibious();

    public static final Codec<MovementAmphibious> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "movement.amphibious";
    }
}
