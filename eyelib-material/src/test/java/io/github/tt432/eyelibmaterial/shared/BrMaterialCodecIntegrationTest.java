package io.github.tt432.eyelibmaterial.shared;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying full vanilla.material JSON parsing with
 * field-level assertions on every record component.
 * <p>
 * This test goes beyond roundtrip tests by asserting specific decoded values
 * of each field within {@link BrMaterialEntry} and its nested records.
 *
 * @author TT432
 */
class BrMaterialCodecIntegrationTest {

    /**
     * The vanilla.material content as embedded JSON string.
     * Source: {@code src/main/resources/assets/eyelib/eyelib/materials/vanilla.material}
     * in the root project.
     */
    private static final String VANILLA_MATERIAL_JSON = """
            {
              "materials": {
                "cutout": {
                  "vertexShader": "a",
                  "fragmentShader": "b",
                  "defines": [],
                  "samplerStates": [],
                  "states": [],
                  "variants": []
                },
                "translucent": {
                  "vertexShader": "a",
                  "fragmentShader": "b",
                  "defines": [],
                  "samplerStates": [],
                  "states": [],
                  "variants": []
                },
                "solid": {
                  "vertexShader": "a",
                  "fragmentShader": "b",
                  "defines": [],
                  "samplerStates": [],
                  "states": [],
                  "variants": []
                }
              }
            }
            """;

    @Test
    @DisplayName("Parse vanilla.material and verify all entry field values")
    void parseVanillaMaterial() {
        var jsonElement = JsonParser.parseString(VANILLA_MATERIAL_JSON);
        var jsonObject = jsonElement.getAsJsonObject();

        var result = BrMaterial.CODEC.parse(JsonOps.INSTANCE, jsonObject);
        var material = result.getOrThrow(false,
                msg -> new AssertionError("Parse failed: " + msg));

        assertEquals(3, material.materials().size(),
                "vanilla.material should contain exactly 3 entries");

        verifyEntry(material, "cutout");
        verifyEntry(material, "translucent");
        verifyEntry(material, "solid");
    }

    /**
     * Asserts every field of the named material entry matches the expected
     * vanilla.material values.
     *
     * @param material the fully decoded {@link BrMaterial}
     * @param name     the entry key (e.g. "cutout")
     */
    private static void verifyEntry(BrMaterial material, String name) {
        var entry = material.materials().get(name);
        assertNotNull(entry, "Entry should exist: " + name);

        // ---- Identity fields ----
        assertEquals(name, entry.name());
        assertEquals("", entry.base(), "base should be empty for top-level entries");

        // ---- Shader fields ----
        assertEquals(java.util.Optional.of("a"), entry.vertexShader());
        assertEquals(java.util.Optional.of("b"), entry.fragmentShader());

        // ---- Defines ----
        var defines = entry.defines();
        assertEquals(java.util.Optional.of(List.of()), defines.base(),
                "defines.base should be present with empty list from []");
        assertTrue(defines.add().isEmpty(), "defines.add should be absent");
        assertTrue(defines.sub().isEmpty(), "defines.sub should be absent");

        // ---- SamplerStates ----
        var samplerStates = entry.samplerStates();
        assertEquals(java.util.Optional.of(List.of()), samplerStates.base(),
                "samplerStates.base should be present with empty list from []");
        assertTrue(samplerStates.add().isEmpty(), "samplerStates.add should be absent");
        assertTrue(samplerStates.sub().isEmpty(), "samplerStates.sub should be absent");

        // ---- States ----
        var states = entry.states();
        assertEquals(java.util.Optional.of(List.of()), states.base(),
                "states.base should be present with empty list from []");
        assertTrue(states.add().isEmpty(), "states.add should be absent");
        assertTrue(states.sub().isEmpty(), "states.sub should be absent");

        // ---- DepthFunc ----
        assertTrue(entry.depthFunc().isEmpty(), "depthFunc should be absent");

        // ---- Blend ----
        var blend = entry.blend();
        assertTrue(blend.blendSrc().isEmpty(), "blendSrc should be absent");
        assertTrue(blend.blendDst().isEmpty(), "blendDst should be absent");
        assertTrue(blend.alphaSrc().isEmpty(), "alphaSrc should be absent");
        assertTrue(blend.alphaDst().isEmpty(), "alphaDst should be absent");

        // ---- Stencil ----
        var stencil = entry.stencil();
        assertTrue(stencil.stencilRef().isEmpty(), "stencilRef should be absent");
        assertTrue(stencil.stencilRefOverride().isEmpty(), "stencilRefOverride should be absent");
        assertTrue(stencil.stencilReadMask().isEmpty(), "stencilReadMask should be absent");
        assertTrue(stencil.stencilWriteMask().isEmpty(), "stencilWriteMask should be absent");
        assertTrue(stencil.frontFace().isEmpty(), "frontFace should be absent");
        assertTrue(stencil.backFace().isEmpty(), "backFace should be absent");

        // ---- VertexFields ----
        assertTrue(entry.vertexFields().isEmpty(), "vertexFields should be absent");

        // ---- Variants ----
        assertEquals(List.of(), entry.variants());
    }
}
