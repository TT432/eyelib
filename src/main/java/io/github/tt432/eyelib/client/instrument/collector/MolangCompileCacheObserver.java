package io.github.tt432.eyelib.client.instrument.collector;

import io.github.tt432.eyelibmolang.compiler.cache.MolangCompileCache;

/**
 * A {@link CacheSizeObserver} that reports the current in-memory entry count
 * of a {@link MolangCompileCache} instance.
 */
public final class MolangCompileCacheObserver implements CacheSizeObserver {
    private final MolangCompileCache cache;

    public MolangCompileCacheObserver(MolangCompileCache cache) {
        this.cache = cache;
    }

    @Override
    public String source() { return "MolangCompileCache"; }

    @Override
    public String metricName() { return "cache_entries"; }

    @Override
    public int currentSize() { return cache.size(); }

    @Override
    public String metricUnit() { return "count"; }
}
