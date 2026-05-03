package io.github.tt432.eyelib.client.instrument.collector;

import io.github.tt432.eyelibmolang.MolangScope;

public final class MolangScopeCacheObserver implements CacheSizeObserver {
    private final MolangScope scope;

    public MolangScopeCacheObserver(MolangScope scope) {
        this.scope = scope;
    }

    @Override
    public String source() { return "MolangScope"; }

    @Override
    public String metricName() { return "scope_cache_entries"; }

    @Override
    public int currentSize() { return scope.getCacheSize(); }

    @Override
    public String metricUnit() { return "count"; }
}
