package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:navigation.float — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record NavigationFloat() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final NavigationFloat INSTANCE = new NavigationFloat();

    public static final Codec<NavigationFloat> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "navigation.float";
    }
}
