package io.github.tt432.eyelibparticle.loading;

import io.github.tt432.eyelibparticle.api.ParticlePublisher;
import io.github.tt432.eyelibparticle.api.ParticleStore;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Module-owned active particle definition registry used by loading/publication seams.
 */
public final class ParticleDefinitionRegistry {
    private static final MemoryParticleDefinitionStore STORE = new MemoryParticleDefinitionStore();
    private static final ParticlePublisher<ParticleDefinition> PUBLISHER =
            new ParticlePublisher<>(STORE, ParticleDefinition::identifier);

    private ParticleDefinitionRegistry() {
    }

    public static ParticleStore<ParticleDefinition> store() {
        return STORE;
    }

    public static ParticlePublisher<ParticleDefinition> publisher() {
        return PUBLISHER;
    }

    private static final class MemoryParticleDefinitionStore implements ParticleStore<ParticleDefinition> {
        private final LinkedHashMap<String, ParticleDefinition> particles = new LinkedHashMap<>();

        @Override
        public ParticleDefinition get(String id) {
            return particles.get(id);
        }

        @Override
        public Map<String, ParticleDefinition> all() {
            return Collections.unmodifiableMap(particles);
        }

        @Override
        public void put(String id, ParticleDefinition particle) {
            particles.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(particle, "particle"));
        }

        @Override
        public void replaceAll(Map<String, ? extends ParticleDefinition> replacement) {
            Objects.requireNonNull(replacement, "replacement");
            particles.clear();
            particles.putAll(replacement);
        }

        @Override
        public void clear() {
            particles.clear();
        }
    }
}
