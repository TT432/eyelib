package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:is_stunned — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsStunned() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final IsStunned INSTANCE = new IsStunned();

    public static final Codec<IsStunned> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_stunned";
    }
}
