package io.github.tt432.eyelib.particle.loading;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.github.tt432.eyelib.particle.runtime.ParticleDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class ParticleResourcePublicationTest {
    @BeforeEach
    void clearRegistry() {
        ParticleDefinitionRegistry.store().clear();
    }

    @Test
    void sourceKeyIsNotPublishedAsActiveIdentifier() {
        ParticleLoadReport report = ParticleResourcePublication.replaceFromJsonResources(resources(
                Map.entry("eyelib:source_first", particleJson("eyelib:first_description")),
                Map.entry("eyelib:source_second", particleJson("eyelib:second_description"))
        ), LoggerFactory.getLogger(ParticleResourcePublicationTest.class));

        assertEquals(List.of("eyelib:source_first", "eyelib:source_second"), report.processedSourceIds());
        assertEquals(List.of("eyelib:first_description", "eyelib:second_description"), report.publishedIdentifiers());
        assertNull(ParticleDefinitionRegistry.store().get("eyelib:source_first"));
        assertNotNull(ParticleDefinitionRegistry.store().get("eyelib:first_description"));
        assertNotNull(ParticleDefinitionRegistry.store().get("eyelib:second_description"));
    }

    @Test
    void fullReplacementRemovesStaleEntries() {
        ParticleResourcePublication.replaceFromJsonResources(resources(
                Map.entry("eyelib:initial", particleJson("eyelib:stale"))
        ), LoggerFactory.getLogger(ParticleResourcePublicationTest.class));

        ParticleResourcePublication.replaceFromJsonResources(resources(
                Map.entry("eyelib:source_first", particleJson("eyelib:first_description")),
                Map.entry("eyelib:source_second", particleJson("eyelib:second_description"))
        ), LoggerFactory.getLogger(ParticleResourcePublicationTest.class));

        assertFalse(ParticleDefinitionRegistry.store().all().containsKey("eyelib:stale"));
        assertEquals(List.of("eyelib:first_description", "eyelib:second_description"),
                List.copyOf(ParticleDefinitionRegistry.store().all().keySet()));
    }

    @Test
    void replacementOrderFollowsValidDefinitionConversionOrder() {
        ParticleLoadReport report = ParticleResourcePublication.replaceFromJsonResources(resources(
                Map.entry("eyelib:source_second", particleJson("eyelib:second_description")),
                Map.entry("eyelib:source_first", particleJson("eyelib:first_description"))
        ), LoggerFactory.getLogger(ParticleResourcePublicationTest.class));

        assertEquals(List.of("eyelib:second_description", "eyelib:first_description"),
                List.copyOf(ParticleDefinitionRegistry.store().all().keySet()));
        assertEquals(List.of("eyelib:second_description", "eyelib:first_description"), report.publishedIdentifiers());
    }

    @Test
    void duplicateIdentifiersAreReportedAndLaterDefinitionReplacesEarlierEntry() {
        ParticleLoadReport report = ParticleResourcePublication.replaceFromJsonResources(resources(
                Map.entry("eyelib:source_first", particleJson("eyelib:duplicate_description", "textures/particle/first")),
                Map.entry("eyelib:source_second", particleJson("eyelib:duplicate_description", "textures/particle/second"))
        ), LoggerFactory.getLogger(ParticleResourcePublicationTest.class));

        ParticleDefinition definition = ParticleDefinitionRegistry.store().get("eyelib:duplicate_description");
        assertNotNull(definition);
        assertEquals("textures/particle/second", definition.texture());
        assertEquals(List.of("eyelib:duplicate_description"), List.copyOf(ParticleDefinitionRegistry.store().all().keySet()));
        assertEquals(List.of("eyelib:duplicate_description"), report.duplicateIdentifiers());
    }

    @Test
    void invalidResourceIsReportedAndSkippedWhileValidEntriesReplaceStore() {
        ParticleResourcePublication.replaceFromJsonResources(resources(
                Map.entry("eyelib:stale_source", particleJson("eyelib:stale"))
        ), LoggerFactory.getLogger(ParticleResourcePublicationTest.class));

        ParticleLoadReport report = ParticleResourcePublication.replaceFromJsonResources(resources(
                Map.entry("eyelib:valid_source", particleJson("eyelib:valid_description")),
                Map.entry("eyelib:invalid_source", invalidParticleJson())
        ), LoggerFactory.getLogger(ParticleResourcePublicationTest.class));

        assertEquals(List.of("eyelib:valid_source", "eyelib:invalid_source"), report.processedSourceIds());
        assertEquals(List.of("eyelib:valid_description"), report.publishedIdentifiers());
        assertEquals(List.of("eyelib:invalid_source"), report.failedSourceIds());
        assertTrue(report.failures().get(0).message().contains("basic_render_parameters"));
        assertFalse(ParticleDefinitionRegistry.store().all().containsKey("eyelib:stale"));
        assertNotNull(ParticleDefinitionRegistry.store().get("eyelib:valid_description"));
        assertNull(ParticleDefinitionRegistry.store().get("eyelib:invalid_source"));
    }

    @SafeVarargs
    private static Map<String, JsonElement> resources(Map.Entry<String, JsonElement>... entries) {
        LinkedHashMap<String, JsonElement> resources = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : entries) {
            resources.put(entry.getKey(), entry.getValue());
        }
        return resources;
    }

    private static JsonElement particleJson(String identifier) {
        return particleJson(identifier, "textures/particle/particles");
    }

    private static JsonElement particleJson(String identifier, String texture) {
        return JsonParser.parseString("""
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
                """.formatted(identifier, texture));
    }

    private static JsonElement invalidParticleJson() {
        return JsonParser.parseString("""
                {
                  "format_version": "1.10.0",
                  "particle_effect": {
                    "description": {
                      "identifier": "eyelib:invalid_description"
                    }
                  }
                }
                """);
    }
}