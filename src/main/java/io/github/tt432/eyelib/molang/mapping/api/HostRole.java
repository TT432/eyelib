package io.github.tt432.eyelib.molang.mapping.api;

import java.util.Objects;

/**
 * 类型安全的主机角色标识符，支持同一 Java 类型的不同语义角色。
 *
 * @param <T> 此角色关联的值类型
 * @author TT432
 */
public final class HostRole<T> {
    // Opt18-D：全局驻留——同 (name, type) 恒返回同一实例并分配稠密序号，
    // HostContext 据此以数组索引替代 HashMap 探测（JFR：HostRole.hashCode ~2% render 线程）。
    // equals/hashCode 语义不变（驻留后身份比较与值比较等价）。
    private static final java.util.concurrent.ConcurrentHashMap<String, HostRole<?>> REGISTRY =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicInteger NEXT_ID =
            new java.util.concurrent.atomic.AtomicInteger();

    private final String name;
    private final Class<T> type;
    private final int id;

    private HostRole(String name, Class<T> type, int id) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.id = id;
    }

    @SuppressWarnings("unchecked")
    public static <T> HostRole<T> of(String name, Class<T> type) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        return (HostRole<T>) REGISTRY.computeIfAbsent(name + '\0' + type.getName(),
                k -> new HostRole<>(name, type, NEXT_ID.getAndIncrement()));
    }

    /** 驻留序号：全局稠密从 0 递增，供 HostContext 数组索引。 */
    public int id() {
        return id;
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