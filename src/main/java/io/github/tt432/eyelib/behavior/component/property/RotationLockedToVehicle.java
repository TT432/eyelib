package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:rotation_locked_to_vehicle — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record RotationLockedToVehicle() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final RotationLockedToVehicle INSTANCE = new RotationLockedToVehicle();

    public static final Codec<RotationLockedToVehicle> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "rotation_locked_to_vehicle";
    }
}
