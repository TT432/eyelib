package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:free_camera_controlled — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record FreeCameraControlled() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final FreeCameraControlled INSTANCE = new FreeCameraControlled();

    public static final Codec<FreeCameraControlled> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "free_camera_controlled";
    }
}
