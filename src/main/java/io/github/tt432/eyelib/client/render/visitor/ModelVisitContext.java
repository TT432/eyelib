package io.github.tt432.eyelib.client.render.visitor;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author TT432
 */
public final class ModelVisitContext {
    private final Map<String, Object> data = new Object2ObjectOpenHashMap<>();

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public void put(ModelVisitContext other) {
        data.putAll(other.data);
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

    public void clear() {
        data.clear();
    }
}
