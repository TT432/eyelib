package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:fire_immune — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record FireImmune() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final FireImmune INSTANCE = new FireImmune();

    public static final Codec<FireImmune> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "fire_immune";
    }
}
