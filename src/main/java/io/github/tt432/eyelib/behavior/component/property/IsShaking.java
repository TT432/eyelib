package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:is_shaking — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsShaking() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final IsShaking INSTANCE = new IsShaking();

    public static final Codec<IsShaking> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_shaking";
    }
}
