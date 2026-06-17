package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:scaffolding_climber — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record ScaffoldingClimber() implements io.github.tt432.eyelibbehavior.component.Component {
    private static final ScaffoldingClimber INSTANCE = new ScaffoldingClimber();

    public static final Codec<ScaffoldingClimber> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "scaffolding_climber";
    }
}
