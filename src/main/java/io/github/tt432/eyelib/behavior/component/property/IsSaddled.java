package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:is_saddled — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsSaddled() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final IsSaddled INSTANCE = new IsSaddled();

    public static final Codec<IsSaddled> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_saddled";
    }
}
