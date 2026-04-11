package io.github.tt432.eyelib.client.manager;

import java.util.Map;

public interface ManagerWritePort<T> {
    void put(String name, T value);

    void replaceAll(Map<String, ? extends T> replacement);

    void clear();
}
