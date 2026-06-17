package io.github.tt432.eyelib.animation;

import org.jspecify.annotations.Nullable;

import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public record RuntimeParticlePlayData(
        String particleUUID,
        @Nullable String locator,
        float startTicks
) {
}
