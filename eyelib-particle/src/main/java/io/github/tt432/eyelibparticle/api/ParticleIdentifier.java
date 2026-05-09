package io.github.tt432.eyelibparticle.api;

/**
 * Extracts the stable string identifier for a particle definition.
 *
 * @param <T> particle definition type supplied by the consuming runtime adapter
 */
@FunctionalInterface
public interface ParticleIdentifier<T> {
    /**
     * Returns the string identifier used to publish a particle definition.
     *
     * @param particle particle definition
     * @return stable string particle identifier
     */
    String identify(T particle);
}
