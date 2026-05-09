package io.github.tt432.eyelibparticle.runtime.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Typed runtime-local key/value storage for moved particle runtime classes.
 */
public final class ParticleBlackboard {
    private final Map<String, Object> data = new HashMap<>();

    public void put(String key, Object value) {
        data.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        Object value = data.get(key);
        return value == null ? Optional.empty() : Optional.of(type.cast(value));
    }

    public <T> T getOrDefault(String key, Class<T> type, T defaultValue) {
        return get(key, type).orElse(defaultValue);
    }

    public <T> T getOrCreate(String key, Class<T> type, T defaultValue) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(defaultValue, "defaultValue");
        return type.cast(data.computeIfAbsent(key, ignored -> defaultValue));
    }
}
