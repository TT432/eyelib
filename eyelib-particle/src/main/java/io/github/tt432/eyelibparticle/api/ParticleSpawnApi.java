package io.github.tt432.eyelibparticle.api;

/**
 * String-keyed spawn/remove request port for particle runtime adapters.
 */
public interface ParticleSpawnApi {
    /**
     * Applies a particle spawn request.
     *
     * @param request string-keyed particle spawn request
     */
    void spawn(ParticleSpawnRequest request);

    /**
     * Removes a spawned particle emitter by string spawn identifier.
     *
     * @param spawnId string spawn identifier
     */
    void remove(String spawnId);
}
