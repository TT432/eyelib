package io.github.tt432.eyelib.client.instrument.collector;

import io.github.tt432.eyelibparticle.client.ParticleRenderManager;

/**
 * Observes the total number of particles and emitters in {@link ParticleRenderManager}.
 * <p>
 * Reports the sum of {@link ParticleRenderManager#getEmitterCount()} and
 * {@link ParticleRenderManager#getParticleCount()} as the current cache size.
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
        return ParticleRenderManager.INSTANCE.getEmitterCount() + ParticleRenderManager.INSTANCE.getParticleCount();
    }

    @Override
    public String metricUnit() {
        return "count";
    }
}
