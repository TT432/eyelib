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
class BrMaterialNewFieldsTest {

    static final String ENTRY_JSON = """
            {
              "materials": {
                "cutout": {
                  "vertexShader": "a",
                  "fragmentShader": "b",
                  "defines": [],
                  "samplerStates": [],
                  "states": [],
                  "msaaSupport": "Both",
                  "depthBias": 0.001,
                  "slopeScaledDepthBias": 0.5,
                  "primitiveMode": "TriangleList",
                  "renderTargetFormats": ["format1", "format2"],
                  "isAnimatedTexture": true,
                  "variants": []
                }
              }
            }
            """;

    static final String VANILLA_MATERIAL_JSON = """
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
    @DisplayName("All new fields roundtrip: JSON → entry → JSON → entry, assert equality")
    void allNewFieldsRoundtrip() {
        var jsonElement = JsonParser.parseString(ENTRY_JSON);
        var jsonObject = jsonElement.getAsJsonObject();

        // First decode: JSON → BrMaterial
        var firstResult = BrMaterial.CODEC.parse(JsonOps.INSTANCE, jsonObject);
        var first = firstResult.getOrThrow(false,
                msg -> new AssertionError("First decode failed: " + msg));

        var entry = first.materials().get("cutout");
        assertNotNull(entry, "Entry 'cutout' should exist");

        // Assert all new fields are present and correct
        assertTrue(entry.msaaSupport().isPresent(), "msaaSupport should be present");
        assertEquals(MsaaSupport.Both, entry.msaaSupport().get());

        assertTrue(entry.depthBias().isPresent(), "depthBias should be present");
        assertEquals(0.001, entry.depthBias().get(), 1e-9);

        assertTrue(entry.slopeScaledDepthBias().isPresent(), "slopeScaledDepthBias should be present");
        assertEquals(0.5, entry.slopeScaledDepthBias().get(), 1e-9);

        assertTrue(entry.primitiveMode().isPresent(), "primitiveMode should be present");
        assertEquals(PrimitiveMode.TriangleList, entry.primitiveMode().get());

        assertTrue(entry.renderTargetFormats().isPresent(), "renderTargetFormats should be present");
        assertEquals(List.of("format1", "format2"), entry.renderTargetFormats().get());

        assertTrue(entry.isAnimatedTexture().isPresent(), "isAnimatedTexture should be present");
        assertTrue(entry.isAnimatedTexture().get());

        // Re-encode: BrMaterial → JSON
        var encodedResult = BrMaterial.CODEC.encodeStart(JsonOps.INSTANCE, first);
        var encoded = encodedResult.getOrThrow(false,
                msg -> new AssertionError("Re-encode failed: " + msg));

        // Second decode: JSON → BrMaterial
        var secondResult = BrMaterial.CODEC.parse(JsonOps.INSTANCE, encoded);
        var second = secondResult.getOrThrow(false,
                msg -> new AssertionError("Second decode failed: " + msg));

        // Assert structural equality
        assertEquals(first, second,
                "Roundtrip: decoded BrMaterial differs after encode→decode");
    }

    @Test
    @DisplayName("Vanilla material (no new fields) still parses correctly")
    void vanillaMaterialWithoutNewFields() {
        var jsonElement = JsonParser.parseString(VANILLA_MATERIAL_JSON);
        var jsonObject = jsonElement.getAsJsonObject();

        var result = BrMaterial.CODEC.parse(JsonOps.INSTANCE, jsonObject);
        var material = result.getOrThrow(false,
                msg -> new AssertionError("Decode failed for vanilla material: " + msg));

        // Verify all entries exist
        assertTrue(material.materials().containsKey("cutout"));
        assertTrue(material.materials().containsKey("translucent"));
        assertTrue(material.materials().containsKey("solid"));

        // Verify all new fields are Optional.empty() in each entry
        for (var entry : material.materials().values()) {
            assertTrue(entry.msaaSupport().isEmpty(),
                    "msaaSupport should be empty in vanilla material");
            assertTrue(entry.depthBias().isEmpty(),
                    "depthBias should be empty in vanilla material");
            assertTrue(entry.slopeScaledDepthBias().isEmpty(),
                    "slopeScaledDepthBias should be empty in vanilla material");
            assertTrue(entry.primitiveMode().isEmpty(),
                    "primitiveMode should be empty in vanilla material");
            assertTrue(entry.renderTargetFormats().isEmpty(),
                    "renderTargetFormats should be empty in vanilla material");
            assertTrue(entry.isAnimatedTexture().isEmpty(),
                    "isAnimatedTexture should be empty in vanilla material");
        }
    }
}