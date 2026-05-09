package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.ParticleManager;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelibparticle.api.ParticlePublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ParticleAssetRegistryPublisherAdapterTest {
    @AfterEach
    void tearDown() {
        ParticleManager.writePort().clear();
    }

    @Test
    void publisherAccessorPublishesThroughModulePublisherSeam() {
        BrParticle particle = testParticle("eyelib:published_particle");

        ParticlePublisher<BrParticle> publisher = ParticleAssetRegistry.publisher();
        publisher.publishParticle(particle);

        assertSame(particle, ParticleManager.store().get("eyelib:published_particle"));
    }

    @Test
    void replaceParticlesStillUsesDescriptionIdentifierInsteadOfSourceKeys() {
        ParticleManager.store().put("eyelib:stale", testParticle("eyelib:stale"));
        BrParticle first = testParticle("eyelib:first_description");
        BrParticle second = testParticle("eyelib:second_description");

        LinkedHashMap<String, BrParticle> sourceKeyed = new LinkedHashMap<>();
        sourceKeyed.put("eyelib:source_first", first);
        sourceKeyed.put("eyelib:source_second", second);

        ParticleAssetRegistry.replaceParticles(sourceKeyed);

        assertNull(ParticleManager.store().get("eyelib:stale"));
        assertNull(ParticleManager.store().get("eyelib:source_first"));
        assertNull(ParticleManager.store().get("eyelib:source_second"));
        assertEquals(List.of("eyelib:first_description", "eyelib:second_description"),
                List.copyOf(ParticleManager.store().all().keySet()));
        assertSame(first, ParticleManager.store().get("eyelib:first_description"));
        assertSame(second, ParticleManager.store().get("eyelib:second_description"));
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
