package io.github.tt432.eyelib.client.instrument.collector;

import io.github.tt432.eyelibmolang.MolangValue;

public final class MolangValueConstantPoolObserver implements CacheSizeObserver {
    @Override
    public String source() { return "MolangValue"; }

    @Override
    public String metricName() { return "constant_pool_entries"; }

    @Override
    public int currentSize() { return MolangValue.getConstantPoolSize(); }

    @Override
    public String metricUnit() { return "count"; }
}
