package io.github.tt432.eyelibparticle.runtime.bedrock;

/**
 * Spawn port used by pure emitter lifecycle code instead of root render-manager singletons.
 */
@FunctionalInterface
public interface ParticleRuntimeSpawner {
    void spawnParticle(BedrockParticleInstance particle);
}
