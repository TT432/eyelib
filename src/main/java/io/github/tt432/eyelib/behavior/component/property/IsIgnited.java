package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:is_ignited — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsIgnited() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final IsIgnited INSTANCE = new IsIgnited();

    public static final Codec<IsIgnited> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_ignited";
    }
}
