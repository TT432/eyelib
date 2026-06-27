package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:out_of_control — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record OutOfControl() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final OutOfControl INSTANCE = new OutOfControl();

    public static final Codec<OutOfControl> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "out_of_control";
    }
}
