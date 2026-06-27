package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:uses_legacy_friction — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record UsesLegacyFriction() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final UsesLegacyFriction INSTANCE = new UsesLegacyFriction();

    public static final Codec<UsesLegacyFriction> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "uses_legacy_friction";
    }
}
