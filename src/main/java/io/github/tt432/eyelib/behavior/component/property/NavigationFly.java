package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:navigation.fly — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record NavigationFly() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final NavigationFly INSTANCE = new NavigationFly();

    public static final Codec<NavigationFly> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "navigation.fly";
    }
}
