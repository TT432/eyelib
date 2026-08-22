package io.github.tt432.eyelib.model;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * @author TT432
 */
public final class ModelVisitContext {
    private final Map<String, Object> data = new Object2ObjectOpenHashMap<>();

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public boolean contains(String key) {
        return data.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T get(String key) {
        if (!data.containsKey(key)) return null;
        return (T) data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T orCreate(String key, T value) {
        return (T) data.computeIfAbsent(key, s -> value);
    }

    /**
     * 惰性版本：键不存在时才调用 supplier 构造默认值，避免命中路径的无效分配。
     */
    @SuppressWarnings("unchecked")
    public <T> T orCreate(String key, java.util.function.Supplier<T> supplier) {
        return (T) data.computeIfAbsent(key, s -> supplier.get());
    }

    public void clear() {
        data.clear();
    }
}
