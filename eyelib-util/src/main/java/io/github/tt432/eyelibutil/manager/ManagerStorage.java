package io.github.tt432.eyelibutil.manager;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于字符串键的通用管理器存储。
 *
 * @author TT432
 */
public final class ManagerStorage<T> {
    private final Map<String, T> data = new LinkedHashMap<>();

    public void put(String name, T value) {
        data.put(name, value);
    }

    @Nullable
    public T get(String name) {
        return data.get(name);
    }

    public Map<String, T> getAllData() {
        return new LinkedHashMap<>(data);
    }

    public void replaceAll(Map<String, ? extends T> replacement) {
        data.clear();
        data.putAll(replacement);
    }

    public void clear() {
        data.clear();
    }
}