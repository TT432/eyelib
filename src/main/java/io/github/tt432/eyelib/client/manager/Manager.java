package io.github.tt432.eyelib.client.manager;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author TT432
 */
public abstract class Manager<T> implements ManagerReadPort<T>, ManagerWritePort<T> {
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

    public void replaceAll(Map<String, ? extends T> replacement) {
        storage.replaceAll(replacement);
    }

    public void clear() {
        storage.clear();
    }
}
