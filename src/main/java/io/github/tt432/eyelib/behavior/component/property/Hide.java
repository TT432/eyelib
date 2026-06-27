package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:hide — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Hide() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final Hide INSTANCE = new Hide();

    public static final Codec<Hide> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "hide";
    }
}
