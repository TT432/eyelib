package io.github.tt432.eyelibanimation;

import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleEmitter;
import org.jspecify.annotations.Nullable;

/**
 * @author TT432
 */
public record RuntimeParticlePlayData(
        String particleUUID,
        BedrockParticleEmitter emitter,
        @Nullable String locator,
        float startTicks
) {
}

