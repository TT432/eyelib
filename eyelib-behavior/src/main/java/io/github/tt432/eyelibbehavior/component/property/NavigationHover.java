package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:navigation.hover — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record NavigationHover() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final NavigationHover INSTANCE = new NavigationHover();

    public static final Codec<NavigationHover> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "navigation.hover";
    }
}
