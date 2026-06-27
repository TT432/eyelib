package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:dimension_bound — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record DimensionBound() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final DimensionBound INSTANCE = new DimensionBound();

    public static final Codec<DimensionBound> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "dimension_bound";
    }
}
