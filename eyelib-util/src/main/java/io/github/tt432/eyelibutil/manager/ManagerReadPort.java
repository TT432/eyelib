package io.github.tt432.eyelibutil.manager;

import org.jspecify.annotations.Nullable;

import java.util.Map;

public interface ManagerReadPort<T> {
    @Nullable
    T get(String name);

    Map<String, T> getAllData();

    String getManagerName();
}
