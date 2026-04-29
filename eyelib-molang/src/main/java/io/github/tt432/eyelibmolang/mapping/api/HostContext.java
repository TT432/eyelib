package io.github.tt432.eyelibmolang.mapping.api;

import java.util.Optional;

/**
 * Host context for typed host object publication and lookup.
 * <p>
 * This is the replacement for {@link io.github.tt432.eyelibmolang.MolangOwnerSet}
 * based raw-class lookup. Consumers publish typed host objects via {@link #put(Class, Object)}
 * and retrieve them via {@link #get(Class)}.
 *
 * @author TT432
 */
public interface HostContext {
    /**
     * Looks up a host object by its type.
     *
     * @param clazz the type to search for
     * @param <T>   the type
     * @return an {@link Optional} containing the matching host object, if present
     */
    <T> Optional<T> get(Class<T> clazz);

    /**
     * Publishes or replaces a host object by its type.
     *
     * @param clazz the type
     * @param value the host object
     * @param <T>   the type
     */
    <T> void put(Class<T> clazz, T value);

    /**
     * Removes a host object by its type.
     *
     * @param clazz the type to remove
     * @param <T>   the type
     */
    <T> void remove(Class<T> clazz);
}
