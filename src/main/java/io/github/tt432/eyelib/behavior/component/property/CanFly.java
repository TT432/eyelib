package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:can_fly — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record CanFly() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final CanFly INSTANCE = new CanFly();

    public static final Codec<CanFly> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "can_fly";
    }
}
