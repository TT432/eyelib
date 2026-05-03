package io.github.tt432.eyelibmolang.mapping.api;

import java.util.Objects;

/**
 * Type-safe host role identifier - the canonical semantic term for host object lookup.
 * This replaces raw {@link Class}-based lookup with compile-time type safety
 * and supports multiple roles of the same Java type with different semantics.
 *
 * @param <T> the value type associated with this role
 * @see HostContext
 */
public final class HostRole<T> {
    private final String name;
    private final Class<T> type;

    private HostRole(String name, Class<T> type) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
    }

    /** Creates a new HostRole with the given descriptive name and type token. */
    public static <T> HostRole<T> of(String name, Class<T> type) {
        return new HostRole<>(name, type);
    }

    /** The class token for type-safe cast. */
    public Class<T> type() {
        return type;
    }

    /** Human-readable role name for diagnostics. */
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HostRole<?> that)) return false;
        return name.equals(that.name) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + type.hashCode();
    }

    @Override
    public String toString() {
        return "HostRole[" + name + ": " + type.getSimpleName() + "]";
    }
}
