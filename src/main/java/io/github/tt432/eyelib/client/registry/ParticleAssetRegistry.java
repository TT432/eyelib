package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.ParticleManager;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleAssetRegistry {
    public static void replaceParticles(Map<?, BrParticle> particles) {
        LinkedHashMap<String, BrParticle> flattened = new LinkedHashMap<>();
        particles.forEach((ignored, particle) -> flattened.put(particle.particleEffect().description().identifier(), particle));
        ParticleManager.INSTANCE.replaceAll(flattened);
    }

    public static void publishParticle(BrParticle particle) {
        ParticleManager.INSTANCE.put(particle.particleEffect().description().identifier(), particle);
    }
}
