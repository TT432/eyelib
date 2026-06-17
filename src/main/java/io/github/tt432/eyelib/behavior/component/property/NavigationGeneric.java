package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:navigation.generic — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record NavigationGeneric() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final NavigationGeneric INSTANCE = new NavigationGeneric();

    public static final Codec<NavigationGeneric> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "navigation.generic";
    }
}
