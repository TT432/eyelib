package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.util.manager.ManagerStorage;
import io.github.tt432.eyelib.util.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * @author TT432
 */
@NullMarked
public abstract class Manager<T> implements Repository<T> {
    private final ManagerStorage<T> storage = new ManagerStorage<>();

    public void put(String name, T value) {
        storage.put(name, value);
        ManagerEventPublishBridge.publishManagerEntryChanged(getManagerName(), name, value);
    }

    @Nullable
    public T get(String name) {
        return storage.get(name);
    }

    public String getManagerName() {
        return getClass().getSimpleName();
    }

    public Map<String, T> getAllData() {
        return storage.getAllData();
    }

    @Override
    public Map<String, T> all() {
        return storage.all();
    }

    public void replaceAll(Map<String, ? extends T> replacement) {
        storage.replaceAll(replacement);
    }

    public void clear() {
        storage.clear();
    }
}
