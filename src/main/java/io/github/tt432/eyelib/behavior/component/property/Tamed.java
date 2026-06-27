package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:is_tamed — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Tamed() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final Tamed INSTANCE = new Tamed();

    public static final Codec<Tamed> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_tamed";
    }
}
