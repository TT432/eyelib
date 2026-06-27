package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:navigation.climb — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record NavigationClimb() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final NavigationClimb INSTANCE = new NavigationClimb();

    public static final Codec<NavigationClimb> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "navigation.climb";
    }
}
