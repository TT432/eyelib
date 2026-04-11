package io.github.tt432.eyelib.client.manager;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface ManagerReadPort<T> {
    @Nullable
    T get(String name);

    Map<String, T> getAllData();

    String getManagerName();
}
