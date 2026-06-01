package io.github.tt432.eyelibanimation;

import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleEmitter;
import org.jspecify.annotations.Nullable;

import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public record RuntimeParticlePlayData(
        String particleUUID,
        BedrockParticleEmitter emitter,
        @Nullable String locator,
        float startTicks
) {
}