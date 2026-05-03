package io.github.tt432.eyelib.client.instrument.collector;

import io.github.tt432.eyelib.client.render.RenderHelper;

public final class RenderHelperDfsModelsObserver implements CacheSizeObserver {
    @Override
    public String source() { return "RenderHelper"; }

    @Override
    public String metricName() { return "dfs_models"; }

    @Override
    public int currentSize() { return RenderHelper.getDfsModelsSize(); }

    @Override
    public String metricUnit() { return "count"; }
}
