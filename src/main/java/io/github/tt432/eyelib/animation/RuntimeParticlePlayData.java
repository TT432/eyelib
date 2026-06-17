package io.github.tt432.eyelib.animation;

import org.jspecify.annotations.Nullable;

/**
 * @author TT432
 */
public record RuntimeParticlePlayData(
        String particleUUID,
        @Nullable String locator,
        float startTicks
) {
}
