package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:navigation.generic — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record NavigationGeneric() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final NavigationGeneric INSTANCE = new NavigationGeneric();

    public static final Codec<NavigationGeneric> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "navigation.generic";
    }
}
