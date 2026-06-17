package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:leashable_to — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record LeashableTo() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final LeashableTo INSTANCE = new LeashableTo();

    public static final Codec<LeashableTo> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "leashable_to";
    }
}
