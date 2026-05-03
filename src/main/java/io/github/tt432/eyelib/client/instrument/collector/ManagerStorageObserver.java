package io.github.tt432.eyelib.client.instrument.collector;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Reports the size of a data source backed by a {@link Map}.
 * Designed to wrap package-private {@code ManagerStorage.getAllData()} via a {@link Supplier},
 * making it testable without direct access to the storage class.
 */
public final class ManagerStorageObserver implements CacheSizeObserver {
    private final Supplier<Map<String, ?>> getAllDataFn;
    private final String sourceName;

    public ManagerStorageObserver(Supplier<Map<String, ?>> getAllDataFn, String sourceName) {
        this.getAllDataFn = getAllDataFn;
        this.sourceName = sourceName;
    }

    @Override
    public String source() {
        return sourceName;
    }

    @Override
    public String metricName() {
        return "allocation_size";
    }

    @Override
    public int currentSize() {
        return getAllDataFn.get().size();
    }

    @Override
    public String metricUnit() {
        return "count";
    }
}
