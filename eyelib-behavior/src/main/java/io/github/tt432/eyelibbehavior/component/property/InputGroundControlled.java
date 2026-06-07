package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:input_ground_controlled — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record InputGroundControlled() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final InputGroundControlled INSTANCE = new InputGroundControlled();

    public static final Codec<InputGroundControlled> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "input_ground_controlled";
    }
}
