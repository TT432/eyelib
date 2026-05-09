package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.ParticleManager;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelibparticle.api.ParticlePublisher;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Transitional root publication facade delegating to {@link ParticlePublisher}.
 * <p>
 * The canonical publication seam lives in {@code io.github.tt432.eyelibparticle.api}. Remove this facade after root
 * loaders/tooling migrate directly to particle API adapters/services.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleAssetRegistry {
    private static final ParticlePublisher<BrParticle> PUBLISHER = new ParticlePublisher<>(
            ParticleManager.store(),
            particle -> particle.particleEffect().description().identifier()
    );

    public static ParticlePublisher<BrParticle> publisher() {
        return PUBLISHER;
    }

    public static void replaceParticles(Map<?, BrParticle> particles) {
        publisher().replaceParticles(particles.values());
    }

    public static void publishParticle(BrParticle particle) {
        publisher().publishParticle(particle);
    }
}
