package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.capability.AnimatableCapability;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
public class MolangScope {
    @Getter
    @Setter
    AnimatableCapability<?> owner = null;
    Map<String, Float> cache = new HashMap<>();
    Map<String, Object> animationData = new HashMap<>();

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
    public <T> T getExtraData(String key, @Nullable T defaultValue) {
        if (animationData.containsKey(key))
            return (T) animationData.get(key);
        return defaultValue;
    }

    public <T> T getOrCreateExtraData(String key, T defaultValue) {
        return (T) animationData.computeIfAbsent(key, s -> defaultValue);
    }

    public void setExtraData(String key, Object value) {
        animationData.put(key, value);
    }
}
