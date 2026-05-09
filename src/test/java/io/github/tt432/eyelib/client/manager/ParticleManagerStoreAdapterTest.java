package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelibparticle.api.ParticleStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ParticleManagerStoreAdapterTest {
    @AfterEach
    void tearDown() {
        ParticleManager.writePort().clear();
    }

    @Test
    void storeAccessorExposesModuleParticleStoreBackedByManagerStorage() {
        BrParticle particle = testParticle("eyelib:test_particle");

        ParticleStore<BrParticle> store = ParticleManager.store();
        store.put("eyelib:test_particle", particle);

        assertSame(particle, ParticleManager.readPort().get("eyelib:test_particle"));
        assertSame(particle, store.get("eyelib:test_particle"));
        assertEquals(Map.of("eyelib:test_particle", particle), store.all());
    }

    @Test
    void storeAccessorPreservesReplacementAndClearBehavior() {
        ParticleStore<BrParticle> store = ParticleManager.store();
        store.put("eyelib:stale", testParticle("eyelib:stale"));

        BrParticle replacement = testParticle("eyelib:replacement");
        LinkedHashMap<String, BrParticle> particles = new LinkedHashMap<>();
        particles.put("eyelib:replacement", replacement);
        store.replaceAll(particles);

        assertNull(ParticleManager.readPort().get("eyelib:stale"));
        assertSame(replacement, ParticleManager.readPort().get("eyelib:replacement"));

        store.clear();
        assertEquals(Map.of(), ParticleManager.readPort().getAllData());
    }

    @Test
    void storeAccessorPreservesPublishedReplacementOrder() {
        ParticleStore<BrParticle> store = ParticleManager.store();

        LinkedHashMap<String, BrParticle> particles = new LinkedHashMap<>();
        particles.put("eyelib:hash_collision_fb", testParticle("eyelib:hash_collision_fb"));
        particles.put("eyelib:hash_collision_ea", testParticle("eyelib:hash_collision_ea"));
        particles.put("eyelib:hash_collision_long_tail", testParticle("eyelib:hash_collision_long_tail"));

        store.replaceAll(particles);

        assertEquals(List.copyOf(particles.keySet()), List.copyOf(store.all().keySet()));
        assertEquals(List.copyOf(particles.keySet()), List.copyOf(store.names()));
    }

    private static BrParticle testParticle(String identifier) {
        return BrParticle.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, com.google.gson.JsonParser.parseString("""
                {
                  "format_version": "1.10.0",
                  "particle_effect": {
                    "description": {
                      "identifier": "%s",
                      "basic_render_parameters": {
                        "material": "particles_alpha",
                        "texture": "%s"
                      }
                    }
                  }
                }
                """.formatted(identifier, identifier))).getOrThrow(false, AssertionError::new);
    }
}
