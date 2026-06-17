package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:out_of_control — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record OutOfControl() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final OutOfControl INSTANCE = new OutOfControl();

    public static final Codec<OutOfControl> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "out_of_control";
    }
}
