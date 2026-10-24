package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:free_camera_controlled — marker component.
 *
 * @author TT432
 */
public record FreeCameraControlled() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final FreeCameraControlled INSTANCE = new FreeCameraControlled();

    public static final Codec<FreeCameraControlled> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "free_camera_controlled";
    }
}
