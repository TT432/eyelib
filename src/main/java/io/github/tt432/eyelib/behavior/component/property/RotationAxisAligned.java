package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:rotation_axis_aligned — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record RotationAxisAligned() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final RotationAxisAligned INSTANCE = new RotationAxisAligned();

    public static final Codec<RotationAxisAligned> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "rotation_axis_aligned";
    }
}
