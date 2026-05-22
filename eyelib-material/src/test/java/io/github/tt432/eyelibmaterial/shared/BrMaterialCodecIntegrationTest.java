package io.github.tt432.eyelibmaterial.shared;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TT432
 */
@NullMarked
class BrMaterialCodecIntegrationTest {

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

    private static void verifyEntry(BrMaterial material, String name) {
        var entry = material.materials().get(name);
        assertNotNull(entry, "Entry should exist: " + name);

        assertEquals(name, entry.name());
        assertEquals("", entry.base(), "base should be empty for top-level entries");

        assertEquals(java.util.Optional.of("a"), entry.vertexShader());
        assertEquals(java.util.Optional.of("b"), entry.fragmentShader());

        var defines = entry.defines();
        assertEquals(java.util.Optional.of(List.of()), defines.base(),
                "defines.base should be present with empty list from []");
        assertTrue(defines.add().isEmpty(), "defines.add should be absent");
        assertTrue(defines.sub().isEmpty(), "defines.sub should be absent");

        var samplerStates = entry.samplerStates();
        assertEquals(java.util.Optional.of(List.of()), samplerStates.base(),
                "samplerStates.base should be present with empty list from []");
        assertTrue(samplerStates.add().isEmpty(), "samplerStates.add should be absent");
        assertTrue(samplerStates.sub().isEmpty(), "samplerStates.sub should be absent");

        var states = entry.states();
        assertEquals(java.util.Optional.of(List.of()), states.base(),
                "states.base should be present with empty list from []");
        assertTrue(states.add().isEmpty(), "states.add should be absent");
        assertTrue(states.sub().isEmpty(), "states.sub should be absent");

        assertTrue(entry.depthFunc().isEmpty(), "depthFunc should be absent");

        var blend = entry.blend();
        assertTrue(blend.blendSrc().isEmpty(), "blendSrc should be absent");
        assertTrue(blend.blendDst().isEmpty(), "blendDst should be absent");
        assertTrue(blend.alphaSrc().isEmpty(), "alphaSrc should be absent");
        assertTrue(blend.alphaDst().isEmpty(), "alphaDst should be absent");

        var stencil = entry.stencil();
        assertTrue(stencil.stencilRef().isEmpty(), "stencilRef should be absent");
        assertTrue(stencil.stencilRefOverride().isEmpty(), "stencilRefOverride should be absent");
        assertTrue(stencil.stencilReadMask().isEmpty(), "stencilReadMask should be absent");
        assertTrue(stencil.stencilWriteMask().isEmpty(), "stencilWriteMask should be absent");
        assertTrue(stencil.frontFace().isEmpty(), "frontFace should be absent");
        assertTrue(stencil.backFace().isEmpty(), "backFace should be absent");

        assertTrue(entry.vertexFields().isEmpty(), "vertexFields should be absent");

        assertEquals(List.of(), entry.variants());
    }
}