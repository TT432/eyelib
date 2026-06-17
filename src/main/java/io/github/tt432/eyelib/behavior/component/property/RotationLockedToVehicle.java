package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:rotation_locked_to_vehicle — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record RotationLockedToVehicle() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final RotationLockedToVehicle INSTANCE = new RotationLockedToVehicle();

    public static final Codec<RotationLockedToVehicle> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "rotation_locked_to_vehicle";
    }
}
