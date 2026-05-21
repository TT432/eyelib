package io.github.tt432.eyelibutil.manager;

import java.util.Map;

/**
 * @author TT432
 */
/** @author TT432 */
public interface ManagerWritePort<T> {
    void put(String name, T value);

    void replaceAll(Map<String, ? extends T> replacement);

    void clear();
}