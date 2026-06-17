package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:is_charged — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsCharged() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final IsCharged INSTANCE = new IsCharged();

    public static final Codec<IsCharged> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_charged";
    }
}
