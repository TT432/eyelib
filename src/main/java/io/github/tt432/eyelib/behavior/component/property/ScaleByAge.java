package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:scale_by_age — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record ScaleByAge() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final ScaleByAge INSTANCE = new ScaleByAge();

    public static final Codec<ScaleByAge> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "scale_by_age";
    }
}
