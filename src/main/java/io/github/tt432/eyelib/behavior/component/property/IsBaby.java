package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:is_baby — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsBaby() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final IsBaby INSTANCE = new IsBaby();

    public static final Codec<IsBaby> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_baby";
    }
}
