package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

/**
 * minecraft:block_climber — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record BlockClimber() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final BlockClimber INSTANCE = new BlockClimber();

    public static final Codec<BlockClimber> CODEC = EyelibCodec.unit(INSTANCE);

    @Override
    public String id() {
        return "block_climber";
    }
}
