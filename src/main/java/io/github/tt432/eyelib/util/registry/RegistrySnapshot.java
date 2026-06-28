package io.github.tt432.eyelib.util.registry;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 不可变快照，表示某一时刻注册中心的全部条目。构造后不可修改。
 *
 * @param <T> 值类型
 * @author TT432
 */
public final class RegistrySnapshot<T> {
    private static final RegistrySnapshot<Object> EMPTY = new RegistrySnapshot<>(Map.of());

    private final Map<String, T> entries;

    private RegistrySnapshot(Map<String, T> entries) {
        this.entries = entries;
    }

    @SuppressWarnings("unchecked")
    public static <T> RegistrySnapshot<T> empty() {
        return (RegistrySnapshot<T>) EMPTY;
    }

    static <T> RegistrySnapshot<T> copyOf(Map<String, ? extends T> source) {
        if (source.isEmpty()) {
            return empty();
        }
        Map<String, T> copy = new LinkedHashMap<>(source);
        return new RegistrySnapshot<>(Collections.unmodifiableMap(copy));
    }

    @Nullable
    public T get(String id) {
        return entries.get(id);
    }

    public Map<String, T> all() {
        return entries;
    }

    public Collection<String> names() {
        return entries.keySet();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    RegistrySnapshot<T> with(String id, T value) {
        Map<String, T> copy = new LinkedHashMap<>(entries);
        copy.put(id, value);
        return new RegistrySnapshot<>(Collections.unmodifiableMap(copy));
    }
}
