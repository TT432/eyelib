package io.github.tt432.eyelib.particle.runtime.bedrock;

/**
 * Spawn port used by pure emitter lifecycle code instead of root render-manager singletons.
 */
@FunctionalInterface
/** @author TT432 */
public interface ParticleRuntimeSpawner {
    void spawnParticle(BedrockParticleInstance particle);
}