package io.github.tt432.eyelibmaterial;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibmaterial.material.BrMaterial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JSON roundtrip regression tests for {@link BrMaterial#CODEC}.
 * <p>
 * TDD RED phase: These tests may currently fail due to incomplete type definitions.
 * They establish a safety net that will turn GREEN when the shared type merge (Task 12)
 * completes without breaking JSON serialization compatibility.
 * <p>
 * Each test loads the full vanilla.material JSON, decodes via CODEC, re-encodes,
 * decodes again, and asserts the two decoded objects are equal.
 */
class BrMaterialCodecRegressionTest {

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

    /**
     * Performs a full encode-decode roundtrip on the vanilla.material JSON.
     *
     * @param entryKey the material entry key to verify exists in the result
     * @return the decoded {@link BrMaterial} (used for additional assertions)
     */
    private static BrMaterial roundtripVanillaMaterial(String entryKey) {
        var jsonElement = JsonParser.parseString(VANILLA_MATERIAL_JSON);
        var jsonObject = jsonElement.getAsJsonObject();

        // 1. First decode: JSON → BrMaterial
        var firstResult = BrMaterial.CODEC.parse(JsonOps.INSTANCE, jsonObject);
        var first = firstResult.getOrThrow(false, msg -> new AssertionError("First decode failed: " + msg));

        // 2. Re-encode: BrMaterial → JSON
        var encodedResult = BrMaterial.CODEC.encodeStart(JsonOps.INSTANCE, first);
        var encoded = encodedResult.getOrThrow(false, msg -> new AssertionError("Re-encode failed: " + msg));

        // 3. Second decode: JSON → BrMaterial
        var secondResult = BrMaterial.CODEC.parse(JsonOps.INSTANCE, encoded);
        var second = secondResult.getOrThrow(false, msg -> new AssertionError("Second decode failed: " + msg));

        // 4. Assert structural equality (record equals)
        assertEquals(first, second,
                "Roundtrip regression: decoded BrMaterial differs after encode→decode for entry: " + entryKey);

        // 5. Assert the requested entry key exists in the materials map
        assertTrue(first.materials().containsKey(entryKey),
                "Material map should contain entry: " + entryKey);

        return first;
    }

    @Test
    @DisplayName("Roundtrip regression: cutout material entry")
    void roundtripCutout() {
        roundtripVanillaMaterial("cutout");
    }

    @Test
    @DisplayName("Roundtrip regression: translucent material entry")
    void roundtripTranslucent() {
        roundtripVanillaMaterial("translucent");
    }

    @Test
    @DisplayName("Roundtrip regression: solid material entry")
    void roundtripSolid() {
        roundtripVanillaMaterial("solid");
    }
}
