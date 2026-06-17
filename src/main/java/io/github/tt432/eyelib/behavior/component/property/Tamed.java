package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:is_tamed — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Tamed() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final Tamed INSTANCE = new Tamed();

    public static final Codec<Tamed> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_tamed";
    }
}
