package io.github.tt432.eyelib.bridge.molang;

import org.jspecify.annotations.Nullable;

public interface ComponentStoreView {
    void put(String componentKey, Object value);

    @SuppressWarnings("unchecked")
    <T> @Nullable T get(String componentKey);
}
