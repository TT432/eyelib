package io.github.tt432.eyelibparticle.api;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParticlePublisherAndSpawnApiTest {
    @Test
    void publisherFlattensParticlesByExtractedIdentifierInIterationOrder() {
        MemoryParticleStore store = new MemoryParticleStore();
        store.put("eyelib:stale", new TestParticle("eyelib:stale"));
        ParticlePublisher<TestParticle> publisher = new ParticlePublisher<>(store, TestParticle::id);

        TestParticle first = new TestParticle("eyelib:first");
        TestParticle second = new TestParticle("eyelib:second");
        publisher.replaceParticles(List.of(first, second));

        assertEquals(List.of("eyelib:first", "eyelib:second"), List.copyOf(store.all().keySet()));
        assertEquals(first, store.get("eyelib:first"));
        assertEquals(second, store.get("eyelib:second"));
    }

    @Test
    void publisherPublishesSingleParticleByExtractedIdentifier() {
        MemoryParticleStore store = new MemoryParticleStore();
        ParticlePublisher<TestParticle> publisher = new ParticlePublisher<>(store, TestParticle::id);
        TestParticle particle = new TestParticle("eyelib:single");

        publisher.publishParticle(particle);

        assertEquals(particle, store.get("eyelib:single"));
    }

    @Test
    void spawnRequestRequiresStringIdsAndDefensivelyCopiesPosition() {
        Vector3f source = new Vector3f(1F, 2F, 3F);
        ParticleSpawnRequest request = new ParticleSpawnRequest("spawn-id", "not-a-resource-location", source);

        source.set(4F, 5F, 6F);
        request.position().set(7F, 8F, 9F);

        assertEquals("spawn-id", request.spawnId());
        assertEquals("not-a-resource-location", request.particleId());
        assertEquals(new Vector3f(1F, 2F, 3F), request.position());
        assertThrows(NullPointerException.class, () -> new ParticleSpawnRequest(null, "particle", new Vector3f()));
        assertThrows(NullPointerException.class, () -> new ParticleSpawnRequest("spawn", null, new Vector3f()));
        assertThrows(NullPointerException.class, () -> new ParticleSpawnRequest("spawn", "particle", null));
    }

    private record TestParticle(String id) {
    }

    private static final class MemoryParticleStore implements ParticleStore<TestParticle> {
        private final LinkedHashMap<String, TestParticle> particles = new LinkedHashMap<>();

        @Override
        public TestParticle get(String id) {
            return particles.get(id);
        }

        @Override
        public Map<String, TestParticle> all() {
            return particles;
        }

        @Override
        public void put(String id, TestParticle particle) {
            particles.put(id, particle);
        }

        @Override
        public void replaceAll(Map<String, ? extends TestParticle> replacement) {
            particles.clear();
            particles.putAll(replacement);
        }

        @Override
        public void clear() {
            particles.clear();
        }
    }
}
