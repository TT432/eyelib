package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import org.jetbrains.annotations.Nullable;

/**
 * @author TT432
 */
public record RuntimeParticlePlayData(
        String particleUUID,
        BrParticleEmitter emitter,
        @Nullable String locator,
        float startTicks
) {
}
