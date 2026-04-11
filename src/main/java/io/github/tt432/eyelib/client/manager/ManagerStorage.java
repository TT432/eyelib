package io.github.tt432.eyelib.client.manager;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class ManagerStorage<T> {
    private final Map<String, T> data = new HashMap<>();

    void put(String name, T value) {
        data.put(name, value);
    }

    @Nullable
    T get(String name) {
        return data.get(name);
    }

    Map<String, T> getAllData() {
        return new HashMap<>(data);
    }

    void replaceAll(Map<String, ? extends T> replacement) {
        data.clear();
        data.putAll(new LinkedHashMap<>(replacement));
    }

    void clear() {
        data.clear();
    }
}
