package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:body_rotation_always_follows_head — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record BodyRotationAlwaysFollowsHead() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final BodyRotationAlwaysFollowsHead INSTANCE = new BodyRotationAlwaysFollowsHead();

    public static final Codec<BodyRotationAlwaysFollowsHead> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "body_rotation_always_follows_head";
    }
}
