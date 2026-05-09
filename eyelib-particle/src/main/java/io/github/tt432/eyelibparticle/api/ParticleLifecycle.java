package io.github.tt432.eyelibparticle.api;

/**
 * Narrow lifecycle/reset port for particle stores.
 */
public interface ParticleLifecycle {
    /**
     * Clears all currently published particle entries.
     */
    void clear();
}
