package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:is_stackable — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsStackable() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final IsStackable INSTANCE = new IsStackable();

    public static final Codec<IsStackable> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_stackable";
    }
}
