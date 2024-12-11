package io.github.tt432.eyelib.client.manager;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
public abstract class Manager<T> {
    private final Map<String, T> data = new HashMap<>();

    public void put(String name, T value) {
        data.put(name, value);
    }

    @Nullable
    public T get(String name) {
        return data.get(name);
    }
}
