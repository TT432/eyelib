package io.github.tt432.eyelib.client.registry;

import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.client.manager.ParticleManager;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelibparticle.api.ParticlePublisher;
import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Transitional root publication facade delegating to {@link ParticlePublisher}.
 * <p>
 * The canonical publication seam lives in {@code io.github.tt432.eyelibparticle.api}. Remove this facade after root
 * loaders/tooling migrate directly to particle API adapters/services.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleAssetRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticleAssetRegistry.class);

    public static ParticlePublisher<ParticleDefinition> publisher() {
        return ParticleDefinitionRegistry.publisher();
    }

    public static void replaceParticles(Map<?, BrParticle> particles) {
        LinkedHashMap<String, ParticleDefinition> definitions = new LinkedHashMap<>();
        LinkedHashMap<String, BrParticle> compatibilityParticles = new LinkedHashMap<>();
        particles.values().forEach(particle -> toModuleDefinition(particle).ifPresent(definition -> {
            definitions.put(definition.identifier(), definition);
            compatibilityParticles.put(definition.identifier(), particle);
        }));
        ParticleDefinitionRegistry.publisher().replaceParticles(definitions.values());
        ParticleManager.store().replaceAll(compatibilityParticles);
    }

    public static void publishParticle(BrParticle particle) {
        toModuleDefinition(particle).ifPresent(definition -> {
            ParticleDefinitionRegistry.publisher().publishParticle(definition);
            ParticleManager.store().put(definition.identifier(), particle);
        });
    }

    private static Optional<ParticleDefinition> toModuleDefinition(BrParticle particle) {
        return BrParticle.CODEC.encodeStart(JsonOps.INSTANCE, particle)
                .flatMap(json -> io.github.tt432.eyelibimporter.particle.BrParticle.CODEC.parse(JsonOps.INSTANCE, json))
                .flatMap(ParticleDefinitionAdapter::fromSchema)
                .resultOrPartial(message -> LOGGER.error("Couldn't convert legacy root particle: {}", message));
    }
}
