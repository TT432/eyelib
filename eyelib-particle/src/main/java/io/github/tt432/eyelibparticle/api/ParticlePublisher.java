package io.github.tt432.eyelibparticle.api;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Publication seam that flattens particle definitions into a string-keyed {@link ParticleStore}.
 *
 * @param <T> particle definition type supplied by the consuming runtime adapter
 */
public final class ParticlePublisher<T> {
    private final ParticleStore<T> store;
    private final ParticleIdentifier<? super T> identifier;

    /**
     * Creates a publisher backed by the provided store and identifier extractor.
     *
     * @param store      particle store to publish into
     * @param identifier extracts each particle's canonical string identifier
     */
    public ParticlePublisher(ParticleStore<T> store, ParticleIdentifier<? super T> identifier) {
        this.store = Objects.requireNonNull(store, "store");
        this.identifier = Objects.requireNonNull(identifier, "identifier");
    }

    /**
     * Publishes a single particle under the identifier supplied by the identifier extractor.
     *
     * @param particle particle definition to publish
     */
    public void publishParticle(T particle) {
        T checkedParticle = Objects.requireNonNull(particle, "particle");
        store.put(identify(checkedParticle), checkedParticle);
    }

    /**
     * Replaces all particles using identifiers supplied by the identifier extractor.
     * <p>
     * Iteration order is preserved through {@link LinkedHashMap} so replacement order remains stable.
     *
     * @param particles particle definitions to publish
     */
    public void replaceParticles(Iterable<? extends T> particles) {
        Objects.requireNonNull(particles, "particles");
        LinkedHashMap<String, T> replacement = new LinkedHashMap<>();
        for (T particle : particles) {
            T checkedParticle = Objects.requireNonNull(particle, "particle");
            replacement.put(identify(checkedParticle), checkedParticle);
        }
        store.replaceAll(replacement);
    }

    private String identify(T particle) {
        return Objects.requireNonNull(identifier.identify(particle), "particle identifier");
    }
}
