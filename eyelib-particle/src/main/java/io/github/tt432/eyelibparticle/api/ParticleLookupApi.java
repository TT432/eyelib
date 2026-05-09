package io.github.tt432.eyelibparticle.api;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * String-keyed read port for particle definitions owned by the particle module boundary.
 *
 * @param <T> particle definition type supplied by the consuming runtime adapter
 */
public interface ParticleLookupApi<T> {
    /**
     * Looks up a particle definition by its string identifier.
     *
     * @param id string particle identifier
     * @return the particle definition, or {@code null} when no entry is registered for {@code id}
     */
    @Nullable
    T get(String id);

    /**
     * Returns all currently registered particle definitions keyed by string identifier.
     *
     * @return string-keyed particle definitions
     */
    Map<String, T> all();

    /**
     * Returns the registered string identifiers.
     *
     * @return registered particle identifiers
     */
    default Collection<String> names() {
        return all().keySet();
    }
}
