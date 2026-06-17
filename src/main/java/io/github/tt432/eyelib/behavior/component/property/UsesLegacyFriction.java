package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:uses_legacy_friction — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record UsesLegacyFriction() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final UsesLegacyFriction INSTANCE = new UsesLegacyFriction();

    public static final Codec<UsesLegacyFriction> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "uses_legacy_friction";
    }
}
