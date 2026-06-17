package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:can_join_raid — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record CanJoinRaid() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final CanJoinRaid INSTANCE = new CanJoinRaid();

    public static final Codec<CanJoinRaid> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "can_join_raid";
    }
}
