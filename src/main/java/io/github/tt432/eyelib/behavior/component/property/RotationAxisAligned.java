package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:rotation_axis_aligned — marker component.
 *
 * @author TT432
 */
public record RotationAxisAligned() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final RotationAxisAligned INSTANCE = new RotationAxisAligned();

    public static final Codec<RotationAxisAligned> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "rotation_axis_aligned";
    }
}
