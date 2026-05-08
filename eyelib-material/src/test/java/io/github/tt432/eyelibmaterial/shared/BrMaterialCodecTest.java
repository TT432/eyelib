package io.github.tt432.eyelibmaterial.shared;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CODEC roundtrip tests for shared {@link BrMaterial}.
 * <p>
 * Each test loads the vanilla.material JSON, decodes via {@link BrMaterial#CODEC},
 * re-encodes, decodes again, and asserts equality.
 */
class BrMaterialCodecTest {

    /**
     * The vanilla.material content as embedded JSON string.
     * Source: {@code src/main/resources/assets/eyelib/eyelib/materials/vanilla.material}
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
     * @return the decoded {@link BrMaterial}
     */
    private static BrMaterial roundtripVanillaMaterial(String entryKey) {
        var jsonElement = JsonParser.parseString(VANILLA_MATERIAL_JSON);
        var jsonObject = jsonElement.getAsJsonObject();

        // 1. First decode: JSON → BrMaterial
        var firstResult = BrMaterial.CODEC.parse(JsonOps.INSTANCE, jsonObject);
        var first = firstResult.getOrThrow(false,
                msg -> new AssertionError("First decode failed: " + msg));

        // 2. Re-encode: BrMaterial → JSON
        var encodedResult = BrMaterial.CODEC.encodeStart(JsonOps.INSTANCE, first);
        var encoded = encodedResult.getOrThrow(false,
                msg -> new AssertionError("Re-encode failed: " + msg));

        // 3. Second decode: JSON → BrMaterial
        var secondResult = BrMaterial.CODEC.parse(JsonOps.INSTANCE, encoded);
        var second = secondResult.getOrThrow(false,
                msg -> new AssertionError("Second decode failed: " + msg));

        // 4. Assert structural equality
        assertEquals(first, second,
                "Roundtrip: decoded BrMaterial differs after encode→decode for entry: " + entryKey);

        // 5. Assert the requested entry key exists in the materials map
        assertTrue(first.materials().containsKey(entryKey),
                "Material map should contain entry: " + entryKey);

        return first;
    }

    @Test
    @DisplayName("CODEC roundtrip: cutout material entry")
    void roundtripCutout() {
        roundtripVanillaMaterial("cutout");
    }

    @Test
    @DisplayName("CODEC roundtrip: translucent material entry")
    void roundtripTranslucent() {
        roundtripVanillaMaterial("translucent");
    }

    @Test
    @DisplayName("CODEC roundtrip: solid material entry")
    void roundtripSolid() {
        roundtripVanillaMaterial("solid");
    }
}
