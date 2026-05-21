package io.github.tt432.eyelibparticle.api;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** @author TT432 */
class ParticleStoreContractTest {
    @Test
    void storeExposesStringKeyedLookupReplacementAndLifecycle() {
        MemoryParticleStore store = new MemoryParticleStore();
        store.put("eyelib:stale", "old");

        LinkedHashMap<String, String> replacement = new LinkedHashMap<>();
        replacement.put("eyelib:first", "first");
        replacement.put("eyelib:second", "second");
        store.replaceAll(replacement);

        assertNull(store.get("eyelib:stale"));
        assertEquals("first", store.get("eyelib:first"));
        assertEquals("second", store.get("eyelib:second"));
        assertEquals(replacement.keySet(), store.names());

        store.clear();

        assertEquals(Map.of(), store.all());
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