package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:floats_in_liquid — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record FloatsInLiquid() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final FloatsInLiquid INSTANCE = new FloatsInLiquid();

    public static final Codec<FloatsInLiquid> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "floats_in_liquid";
    }
}
