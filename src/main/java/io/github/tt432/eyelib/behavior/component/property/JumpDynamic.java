package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:jump.dynamic — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record JumpDynamic() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final JumpDynamic INSTANCE = new JumpDynamic();

    public static final Codec<JumpDynamic> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "jump.dynamic";
    }
}
