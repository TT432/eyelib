package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:persistent — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Persistent() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final Persistent INSTANCE = new Persistent();

    public static final Codec<Persistent> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "persistent";
    }
}
