package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.event.ManagerEntryChangedEvent;
import net.minecraftforge.common.MinecraftForge;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
public abstract class Manager<T> {
    private final Map<String, T> data = new HashMap<>();

    public void put(String name, T value) {
        data.put(name, value);
        MinecraftForge.EVENT_BUS.post(new ManagerEntryChangedEvent(getManagerName(), name, value));
    }

    @Nullable
    public T get(String name) {
        return data.get(name);
    }

    public String getManagerName() {
        return getClass().getSimpleName();
    }

    public Map<String, T> getAllData() {
        return new HashMap<>(data);
    }

    public void replaceAll(Map<String, ? extends T> replacement) {
        data.clear();
        data.putAll(new LinkedHashMap<>(replacement));
    }

    public void clear() {
        data.clear();
    }
}
