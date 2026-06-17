package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:input_ground_controlled — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record InputGroundControlled() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final InputGroundControlled INSTANCE = new InputGroundControlled();

    public static final Codec<InputGroundControlled> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "input_ground_controlled";
    }
}
