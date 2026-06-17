package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:input_air_controlled — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record InputAirControlled() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final InputAirControlled INSTANCE = new InputAirControlled();

    public static final Codec<InputAirControlled> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "input_air_controlled";
    }
}
