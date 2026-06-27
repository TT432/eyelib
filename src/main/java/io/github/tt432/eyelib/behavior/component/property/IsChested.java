package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:is_chested — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsChested() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final IsChested INSTANCE = new IsChested();

    public static final Codec<IsChested> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_chested";
    }
}
