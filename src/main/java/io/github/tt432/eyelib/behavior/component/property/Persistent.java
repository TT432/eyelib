package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:persistent — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Persistent() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final Persistent INSTANCE = new Persistent();

    public static final Codec<Persistent> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "persistent";
    }
}
