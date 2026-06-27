package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:renders_when_invisible — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record RendersWhenInvisible() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final RendersWhenInvisible INSTANCE = new RendersWhenInvisible();

    public static final Codec<RendersWhenInvisible> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "renders_when_invisible";
    }
}
