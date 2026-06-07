package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:renders_when_invisible — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record RendersWhenInvisible() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final RendersWhenInvisible INSTANCE = new RendersWhenInvisible();

    public static final Codec<RendersWhenInvisible> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "renders_when_invisible";
    }
}
