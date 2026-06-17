package io.github.tt432.eyelib.particle.api;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** @author TT432 */
class ParticlePublisherTest {
    @Test
    void replaceParticlesOverwritesStaleEntriesAndUsesExtractedIdentifiers() {
        MemoryParticleStore store = new MemoryParticleStore();
        store.put("loader:stale", "stale-value");
        Map<String, String> ids = Map.of(
                "loader:first", "eyelib:first_particle",
                "loader:second", "eyelib:second_particle"
        );
        ParticlePublisher<String> publisher = new ParticlePublisher<>(store, ids::get);

        publisher.replaceParticles(List.of("loader:first", "loader:second"));

        assertFalse(store.all().containsKey("loader:stale"));
        assertFalse(store.all().containsKey("loader:first"));
        assertEquals(List.of("eyelib:first_particle", "eyelib:second_particle"), List.copyOf(store.all().keySet()));
        assertEquals("loader:first", store.get("eyelib:first_particle"));
        assertEquals("loader:second", store.get("eyelib:second_particle"));
    }

    @Test
    void publishParticleWritesOneEntryUnderExtractedIdentifier() {
        MemoryParticleStore store = new MemoryParticleStore();
        ParticlePublisher<String> publisher = new ParticlePublisher<>(store, value -> "eyelib:" + value);

        publisher.publishParticle("single_particle");

        assertEquals(Map.of("eyelib:single_particle", "single_particle"), store.all());
    }

    private static final class MemoryParticleStore implements ParticleStore<String> {
        private final LinkedHashMap<String, String> particles = new LinkedHashMap<>();

        @Override
        public String get(String id) {
            return particles.get(id);
        }

        @Override
        public Map<String, String> all() {
            return particles;
        }

        @Override
        public void put(String id, String particle) {
            particles.put(id, particle);
        }

        @Override
        public void replaceAll(Map<String, ? extends String> replacement) {
            particles.clear();
            particles.putAll(replacement);
        }

        @Override
        public void clear() {
            particles.clear();
        }
    }
}