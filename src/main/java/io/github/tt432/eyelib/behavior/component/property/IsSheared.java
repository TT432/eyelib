package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:is_sheared — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsSheared() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final IsSheared INSTANCE = new IsSheared();

    public static final Codec<IsSheared> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_sheared";
    }
}
