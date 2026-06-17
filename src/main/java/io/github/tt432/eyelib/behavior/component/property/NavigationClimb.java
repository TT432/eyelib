package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:navigation.climb — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record NavigationClimb() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final NavigationClimb INSTANCE = new NavigationClimb();

    public static final Codec<NavigationClimb> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "navigation.climb";
    }
}
