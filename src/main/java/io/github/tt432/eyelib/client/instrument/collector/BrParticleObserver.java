package io.github.tt432.eyelib.client.instrument.collector;

import io.github.tt432.eyelib.client.particle.bedrock.BrParticleRenderManager;

/**
 * Observes the total number of particles and emitters in {@link BrParticleRenderManager}.
 * <p>
 * Reports the sum of {@link BrParticleRenderManager#getEmitterCount()} and
 * {@link BrParticleRenderManager#getParticleCount()} as the current cache size.
 */
public final class BrParticleObserver implements CacheSizeObserver {
    @Override
    public String source() {
        return "BrParticleRenderManager";
    }

    @Override
    public String metricName() {
        return "total_particles";
    }

    @Override
    public int currentSize() {
        return BrParticleRenderManager.getEmitterCount() + BrParticleRenderManager.getParticleCount();
    }

    @Override
    public String metricUnit() {
        return "count";
    }
}
