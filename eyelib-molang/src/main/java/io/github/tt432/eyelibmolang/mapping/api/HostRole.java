package io.github.tt432.eyelibmolang.mapping.api;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;

/**
 * 类型安全的主机角色标识符，支持同一 Java 类型的不同语义角色。
 *
 * @param <T> 此角色关联的值类型
 * @author TT432
 */
@NullMarked
/** @author TT432 */
public final class HostRole<T> {
    private final String name;
    private final Class<T> type;

    private HostRole(String name, Class<T> type) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
    }

    public static <T> HostRole<T> of(String name, Class<T> type) {
        return new HostRole<>(name, type);
    }

    public Class<T> type() {
        return type;
    }

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