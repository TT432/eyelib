package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:is_dyeable — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsDyeable() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final IsDyeable INSTANCE = new IsDyeable();

    public static final Codec<IsDyeable> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_dyeable";
    }
}
