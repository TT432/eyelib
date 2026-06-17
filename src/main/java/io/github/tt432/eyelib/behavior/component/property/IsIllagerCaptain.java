package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:is_illager_captain — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsIllagerCaptain() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final IsIllagerCaptain INSTANCE = new IsIllagerCaptain();

    public static final Codec<IsIllagerCaptain> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_illager_captain";
    }
}
