package io.github.tt432.eyelibmaterial;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibmaterial.material.BrMaterial;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TT432
 */
@NullMarked
class BrMaterialCodecRegressionTest {

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