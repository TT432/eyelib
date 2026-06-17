package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:ravager_blocked — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record RavagerBlocked() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final RavagerBlocked INSTANCE = new RavagerBlocked();

    public static final Codec<RavagerBlocked> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "ravager_blocked";
    }
}
