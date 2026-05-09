package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.ParticleManager;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ParticleAssetRegistryTest {
    @AfterEach
    void tearDown() {
        ParticleManager.writePort().clear();
    }

    @Test
    void replaceParticlesPublishesByDescriptionIdentifierNotSourceKey() {
        ParticleManager.store().put("eyelib:stale", testParticle("eyelib:stale"));
        BrParticle particle = testParticle("eyelib:description_identifier");

        LinkedHashMap<String, BrParticle> sourceKeyedParticles = new LinkedHashMap<>();
        sourceKeyedParticles.put("eyelib:loader_source_key", particle);

        ParticleAssetRegistry.replaceParticles(sourceKeyedParticles);

        assertNull(ParticleManager.store().get("eyelib:stale"));
        assertNull(ParticleManager.store().get("eyelib:loader_source_key"));
        assertSame(particle, ParticleManager.store().get("eyelib:description_identifier"));
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
