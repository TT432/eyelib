package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:is_hidden_when_invisible — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsHiddenWhenInvisible() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final IsHiddenWhenInvisible INSTANCE = new IsHiddenWhenInvisible();

    public static final Codec<IsHiddenWhenInvisible> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_hidden_when_invisible";
    }
}
