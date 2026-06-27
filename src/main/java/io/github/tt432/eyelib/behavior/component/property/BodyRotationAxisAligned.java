package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:body_rotation_axis_aligned — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record BodyRotationAxisAligned() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final BodyRotationAxisAligned INSTANCE = new BodyRotationAxisAligned();

    public static final Codec<BodyRotationAxisAligned> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "body_rotation_axis_aligned";
    }
}
