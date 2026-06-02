package io.github.tt432.eyelibutil.collection;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于 HashMap 的通用黑板存储，支持按字符串键存取任意类型值。
 *
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
    @Nullable
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrCreate(String key, T defaultValue) {
        return (T) data.computeIfAbsent(key, k -> defaultValue);
    }
}