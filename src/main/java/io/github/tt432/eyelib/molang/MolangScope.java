package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.capability.AnimatableComponent;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
public class MolangScope {
    @Getter
    @Setter
    private AnimatableComponent<?> owner = null;
    private final Map<String, Float> cache = new HashMap<>();
    private final Map<String, Object> animationData = new HashMap<>();

    public float get(String name) {
        return cache.getOrDefault(name, 1F);
    }

    public boolean getBool(String name) {
        return get(name) != MolangValue.FALSE;
    }

    public float set(String name, float value) {
        cache.put(name, value);
        return value;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getExtraData(String key) {
        return (T) animationData.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getExtraData(String key, @NotNull T defaultValue) {
        return (T) animationData.getOrDefault(key, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrCreateExtraData(String key, T defaultValue) {
        return (T) animationData.computeIfAbsent(key, s -> defaultValue);
    }

    public void setExtraData(String key, Object value) {
        animationData.put(key, value);
    }
}
