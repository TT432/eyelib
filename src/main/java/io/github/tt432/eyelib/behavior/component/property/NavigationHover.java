package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:navigation.hover — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record NavigationHover() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final NavigationHover INSTANCE = new NavigationHover();

    public static final Codec<NavigationHover> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "navigation.hover";
    }
}
