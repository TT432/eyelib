package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.ParticleManager;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelibparticle.api.ParticlePublisher;
import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;
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
        ParticleDefinitionRegistry.store().clear();
    }

    @Test
    void publisherAccessorPublishesThroughModuleDefinitionPublisherSeam() {
        ParticleDefinition particle = moduleDefinition("eyelib:published_particle");

        ParticlePublisher<ParticleDefinition> publisher = ParticleAssetRegistry.publisher();
        publisher.publishParticle(particle);

        assertSame(particle, ParticleDefinitionRegistry.store().get("eyelib:published_particle"));
    }

    @Test
    void replaceParticlesStillUsesDescriptionIdentifierInsteadOfSourceKeys() {
        ParticleDefinitionRegistry.store().put("eyelib:stale", moduleDefinition("eyelib:stale"));
        BrParticle first = testParticle("eyelib:first_description");
        BrParticle second = testParticle("eyelib:second_description");

        LinkedHashMap<String, BrParticle> sourceKeyed = new LinkedHashMap<>();
        sourceKeyed.put("eyelib:source_first", first);
        sourceKeyed.put("eyelib:source_second", second);

        ParticleAssetRegistry.replaceParticles(sourceKeyed);

        assertNull(ParticleDefinitionRegistry.store().get("eyelib:stale"));
        assertNull(ParticleDefinitionRegistry.store().get("eyelib:source_first"));
        assertNull(ParticleDefinitionRegistry.store().get("eyelib:source_second"));
        assertEquals(List.of("eyelib:first_description", "eyelib:second_description"),
                List.copyOf(ParticleDefinitionRegistry.store().all().keySet()));
        assertSame(first, ParticleManager.store().get("eyelib:first_description"));
        assertSame(second, ParticleManager.store().get("eyelib:second_description"));
    }

    private static ParticleDefinition moduleDefinition(String identifier) {
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
