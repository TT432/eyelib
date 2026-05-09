package io.github.tt432.eyelibparticle.api;

import java.util.Map;

/**
 * String-keyed mutable particle store port.
 *
 * @param <T> particle definition type supplied by the consuming runtime adapter
 */
public interface ParticleStore<T> extends ParticleLookupApi<T>, ParticleLifecycle {
    /**
     * Publishes or replaces a single particle definition under its string identifier.
     *
     * @param id       string particle identifier
     * @param particle particle definition
     */
    void put(String id, T particle);

    /**
     * Replaces the entire store with the provided string-keyed particle definitions.
     *
     * @param replacement replacement particle definitions keyed by string identifier
     */
    void replaceAll(Map<String, ? extends T> replacement);
}
