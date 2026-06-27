package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:body_rotation_always_follows_head — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record BodyRotationAlwaysFollowsHead() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final BodyRotationAlwaysFollowsHead INSTANCE = new BodyRotationAlwaysFollowsHead();

    public static final Codec<BodyRotationAlwaysFollowsHead> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "body_rotation_always_follows_head";
    }
}
