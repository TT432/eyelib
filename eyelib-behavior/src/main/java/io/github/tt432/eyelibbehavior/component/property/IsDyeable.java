package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:is_dyeable — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsDyeable() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final IsDyeable INSTANCE = new IsDyeable();

    public static final Codec<IsDyeable> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_dyeable";
    }
}
