package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:is_stackable — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsStackable() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final IsStackable INSTANCE = new IsStackable();

    public static final Codec<IsStackable> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_stackable";
    }
}
