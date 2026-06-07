package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:body_rotation_blocked — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record BodyRotationBlocked() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final BodyRotationBlocked INSTANCE = new BodyRotationBlocked();

    public static final Codec<BodyRotationBlocked> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "body_rotation_blocked";
    }
}
