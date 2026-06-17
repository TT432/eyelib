package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:block_climber — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record BlockClimber() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final BlockClimber INSTANCE = new BlockClimber();

    public static final Codec<BlockClimber> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "block_climber";
    }
}
