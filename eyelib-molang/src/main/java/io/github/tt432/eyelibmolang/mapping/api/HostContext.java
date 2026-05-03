package io.github.tt432.eyelibmolang.mapping.api;

import java.util.Optional;

/**
 * Host context for typed host object publication and lookup.
 * <p>
 * This is the replacement for legacy {@code MolangOwnerSet}
 * based raw-class lookup. Consumers publish typed host objects via {@link #put(HostRole, Object)}
 * and retrieve them via {@link #get(HostRole)}.
 *
 * @author TT432
 */
public interface HostContext {
    /**
     * Looks up a host object by its HostRole.
     *
     * @param role the typed role to search for
     * @param <T>  the value type
     * @return an Optional containing the matching host object
     */
    <T> Optional<T> get(HostRole<T> role);

    /**
     * Publishes a host object by its HostRole.
     *
     * @param role  the typed role
     * @param value the host object
     * @param <T>   the value type
     */
    <T> void put(HostRole<T> role, T value);

    /**
     * Removes a host object by its HostRole.
     *
     * @param role the typed role to remove
     * @param <T>  the value type
     */
    <T> void remove(HostRole<T> role);

    /**
     * Looks up a host object by its type.
     *
     * @param clazz the type to search for
     * @param <T>   the type
     * @return an {@link Optional} containing the matching host object, if present
     * @deprecated Use {@link #get(HostRole)} for type-safe role-based lookup.
     */
    @Deprecated
    <T> Optional<T> get(Class<T> clazz);

    /**
     * Publishes or replaces a host object by its type.
     *
     * @param clazz the type
     * @param value the host object
     * @param <T>   the type
     * @deprecated Use {@link #put(HostRole, Object)} for type-safe role-based publication.
     */
    @Deprecated
    <T> void put(Class<T> clazz, T value);

    /**
     * Removes a host object by its type.
     *
     * @param clazz the type to remove
     * @param <T>   the type
     * @deprecated Use {@link #remove(HostRole)} for type-safe role-based removal.
     */
    @Deprecated
    <T> void remove(Class<T> clazz);
}
