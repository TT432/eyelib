package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:is_illager_captain — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsIllagerCaptain() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final IsIllagerCaptain INSTANCE = new IsIllagerCaptain();

    public static final Codec<IsIllagerCaptain> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_illager_captain";
    }
}
