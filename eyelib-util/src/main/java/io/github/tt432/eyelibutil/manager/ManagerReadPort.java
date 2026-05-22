package io.github.tt432.eyelibutil.manager;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * @author TT432
 */
public interface ManagerReadPort<T> {
    @Nullable
    T get(String name);

    Map<String, T> getAllData();

    String getManagerName();
}