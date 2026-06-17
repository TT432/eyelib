package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:can_climb — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record CanClimb() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final CanClimb INSTANCE = new CanClimb();

    public static final Codec<CanClimb> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "can_climb";
    }
}
