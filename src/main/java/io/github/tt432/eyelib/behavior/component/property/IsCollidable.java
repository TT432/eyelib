package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:is_collidable — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record IsCollidable() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final IsCollidable INSTANCE = new IsCollidable();

    public static final Codec<IsCollidable> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "is_collidable";
    }
}
