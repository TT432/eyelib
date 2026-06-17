package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:is_pregnant — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsPregnant() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final IsPregnant INSTANCE = new IsPregnant();

    public static final Codec<IsPregnant> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_pregnant";
    }
}
