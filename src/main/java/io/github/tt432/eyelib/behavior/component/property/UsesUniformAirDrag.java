package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:uses_uniform_air_drag — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record UsesUniformAirDrag() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final UsesUniformAirDrag INSTANCE = new UsesUniformAirDrag();

    public static final Codec<UsesUniformAirDrag> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "uses_uniform_air_drag";
    }
}
