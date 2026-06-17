package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:is_ignited — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsIgnited() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final IsIgnited INSTANCE = new IsIgnited();

    public static final Codec<IsIgnited> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_ignited";
    }
}
