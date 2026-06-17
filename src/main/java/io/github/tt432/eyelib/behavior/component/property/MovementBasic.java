package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:movement.basic — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record MovementBasic() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final MovementBasic INSTANCE = new MovementBasic();

    public static final Codec<MovementBasic> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "movement.basic";
    }
}
