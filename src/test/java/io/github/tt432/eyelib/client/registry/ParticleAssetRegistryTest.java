package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.ParticleManager;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ParticleAssetRegistryTest {
    @AfterEach
    void tearDown() {
        ParticleManager.writePort().clear();
        ParticleDefinitionRegistry.store().clear();
    }

    @Test
    void replaceParticlesPublishesByDescriptionIdentifierNotSourceKey() {
        ParticleDefinitionRegistry.store().put("eyelib:stale", moduleDefinition("eyelib:stale"));
        BrParticle particle = testParticle("eyelib:description_identifier");

        LinkedHashMap<String, BrParticle> sourceKeyedParticles = new LinkedHashMap<>();
        sourceKeyedParticles.put("eyelib:loader_source_key", particle);

        ParticleAssetRegistry.replaceParticles(sourceKeyedParticles);

        assertNull(ParticleDefinitionRegistry.store().get("eyelib:stale"));
        assertNull(ParticleDefinitionRegistry.store().get("eyelib:loader_source_key"));
        assertNotNull(ParticleDefinitionRegistry.store().get("eyelib:description_identifier"));
        assertEquals("eyelib:description_identifier",
                ParticleDefinitionRegistry.store().get("eyelib:description_identifier").identifier());
    }

    private static io.github.tt432.eyelibparticle.runtime.ParticleDefinition moduleDefinition(String identifier) {
        ParticleAssetRegistry.replaceParticles(Map.of(identifier, testParticle(identifier)));
        return ParticleDefinitionRegistry.store().get(identifier);
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
