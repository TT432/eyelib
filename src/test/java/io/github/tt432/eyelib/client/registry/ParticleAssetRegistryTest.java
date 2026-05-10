package io.github.tt432.eyelib.client.registry;

import com.google.gson.JsonParser;
import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;
import io.github.tt432.eyelibparticle.loading.ParticleResourcePublication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ParticleAssetRegistryTest {
    @AfterEach
    void tearDown() {
        ParticleDefinitionRegistry.store().clear();
    }

    @Test
    void rootParticleAssetRegistryFacadeHasBeenRemoved() {
        assertFalse(Files.exists(Path.of("src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java")));
    }

    @Test
    void modulePublicationReplacesLegacyRegistryPublicationByDescriptionIdentifier() {
        ParticleDefinitionRegistry.store().put("eyelib:stale", moduleDefinition("eyelib:stale"));

        LinkedHashMap<String, com.google.gson.JsonElement> sourceKeyedParticles = new LinkedHashMap<>();
        sourceKeyedParticles.put("eyelib:loader_source_key",
                JsonParser.parseString(particleJson("eyelib:description_identifier")));

        ParticleResourcePublication.replaceFromJsonResources(
                sourceKeyedParticles,
                LoggerFactory.getLogger(ParticleAssetRegistryTest.class)
        );

        assertNull(ParticleDefinitionRegistry.store().get("eyelib:stale"));
        assertNull(ParticleDefinitionRegistry.store().get("eyelib:loader_source_key"));
        assertNotNull(ParticleDefinitionRegistry.store().get("eyelib:description_identifier"));
        assertEquals("eyelib:description_identifier",
                ParticleDefinitionRegistry.store().get("eyelib:description_identifier").identifier());
    }

    @Test
    void modulePublisherReplacesPublisherFacadeForDirectDefinitionPublication() {
        ParticleDefinitionRegistry.publisher().publishParticle(moduleDefinition("eyelib:published_definition"));

        assertEquals(List.of("eyelib:published_definition"),
                List.copyOf(ParticleDefinitionRegistry.store().all().keySet()));
    }

    private static io.github.tt432.eyelibparticle.runtime.ParticleDefinition moduleDefinition(String identifier) {
        ParticleResourcePublication.replaceFromJsonResources(
                Map.of(identifier, JsonParser.parseString(particleJson(identifier))),
                LoggerFactory.getLogger(ParticleAssetRegistryTest.class)
        );
        return ParticleDefinitionRegistry.store().get(identifier);
    }

    private static String particleJson(String identifier) {
        return """
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
                """.formatted(identifier, identifier);
    }
}
