package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:body_rotation_locked_to_vehicle — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record BodyRotationLockedToVehicle() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final BodyRotationLockedToVehicle INSTANCE = new BodyRotationLockedToVehicle();

    public static final Codec<BodyRotationLockedToVehicle> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "body_rotation_locked_to_vehicle";
    }
}
