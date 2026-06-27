package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:scaffolding_climber — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record ScaffoldingClimber() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final ScaffoldingClimber INSTANCE = new ScaffoldingClimber();

    public static final Codec<ScaffoldingClimber> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "scaffolding_climber";
    }
}
