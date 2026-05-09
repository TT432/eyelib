package io.github.tt432.eyelib.client.particle.bedrock;

import io.github.tt432.eyelib.client.particle.ParticleSpawnService;
import io.github.tt432.eyelibparticle.client.ParticleRenderManager;
import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleEmitter;
import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleInstance;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Transitional root render-manager adapter for callers that still reference the old root path.
 * <p>
 * Remove this adapter after root animation, packet, instrumentation, and runtime callers migrate directly to
 * {@link ParticleRenderManager} or particle-module API services.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BrParticleRenderManager {
    public static int getEmitterCount() {
        return ParticleRenderManager.INSTANCE.getEmitterCount();
    }

    public static int getParticleCount() {
        return ParticleRenderManager.INSTANCE.getParticleCount();
    }

    public static void spawnEmitter(final String id, final BedrockParticleEmitter emitter) {
        ParticleRenderManager.INSTANCE.spawnEmitter(id, emitter);
    }

    /**
     * Compatibility bridge for legacy root emitters constructed before direct module binding.
     */
    public static void spawnEmitter(final String id, final BrParticleEmitter emitter) {
        ParticleSpawnService.spawnEmitter(id, emitter);
    }

    public static void removeEmitter(final String id) {
        ParticleRenderManager.INSTANCE.removeEmitter(id);
    }

    public static void spawnParticle(final BedrockParticleInstance particle) {
        ParticleRenderManager.INSTANCE.spawnParticle(particle);
    }

    /**
     * Legacy root particle instances are produced only by old root emitters. New registered emitters use module-owned
     * {@link BedrockParticleInstance} objects, so this method is retained only to keep old sources compiling.
     */
    public static void spawnParticle(final BrParticleParticle particle) {
    }
}
