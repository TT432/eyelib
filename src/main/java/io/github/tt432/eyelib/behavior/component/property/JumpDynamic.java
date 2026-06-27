package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:jump.dynamic — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record JumpDynamic() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final JumpDynamic INSTANCE = new JumpDynamic();

    public static final Codec<JumpDynamic> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "jump.dynamic";
    }
}
