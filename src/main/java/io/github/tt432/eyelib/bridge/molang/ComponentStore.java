package io.github.tt432.eyelib.bridge.molang;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 实体组件的运行时存储。所有 System 通过它读写组件数据。
 *
 * @author TT432
 */
@NullMarked
public final class ComponentStore {
    private final Map<String, Object> values = new HashMap<>();

    public void put(String componentKey, Object value) {
        values.put(componentKey, value);
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T get(String componentKey) {
        return (T) values.get(componentKey);
    }
}
