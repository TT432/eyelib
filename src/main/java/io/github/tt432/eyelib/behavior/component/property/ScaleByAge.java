package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:scale_by_age — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record ScaleByAge() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final ScaleByAge INSTANCE = new ScaleByAge();

    public static final Codec<ScaleByAge> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "scale_by_age";
    }
}
