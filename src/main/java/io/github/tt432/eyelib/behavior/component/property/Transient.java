package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:transient — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Transient() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final Transient INSTANCE = new Transient();

    public static final Codec<Transient> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "transient";
    }
}
