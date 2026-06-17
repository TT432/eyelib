package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:can_power_jump — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record CanPowerJump() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final CanPowerJump INSTANCE = new CanPowerJump();

    public static final Codec<CanPowerJump> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "can_power_jump";
    }
}
