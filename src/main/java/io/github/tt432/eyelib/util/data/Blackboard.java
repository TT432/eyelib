package io.github.tt432.eyelib.util.data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
public final class Blackboard {
    private final Map<String, Object> data = new HashMap<>();

    public void put(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        return (T) data.getOrDefault(key, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrCreate(String key, T defaultValue) {
        return (T) data.computeIfAbsent(key, k -> defaultValue);
    }
}
