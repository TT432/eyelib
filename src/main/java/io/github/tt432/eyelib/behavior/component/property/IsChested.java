package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:is_chested — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsChested() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final IsChested INSTANCE = new IsChested();

    public static final Codec<IsChested> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_chested";
    }
}
