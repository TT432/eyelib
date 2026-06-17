package io.github.tt432.eyelib.material.shared;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Merged BrMaterial CODEC tests: roundtrip + entry field verification.
 * 合并自 BrMaterialCodecTest (roundtrip)、BrMaterialCodecRegressionTest (roundtrip)、
 * BrMaterialCodecIntegrationTest (字段级验证)。
 *
 * @author TT432
 */
@NullMarked
class BrMaterialCodecTest {

    private static final String THREE_ENTRY_JSON = """
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

    private static final String EXTENDED_JSON = """
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
                },
                "entity": {
                  "vertexShader": "shaders/render.vert",
                  "fragmentShader": "shaders/render.frag",
                  "defines": [],
                  "samplerStates": [
                    {"samplerIndex": 0, "textureFilter": "Point", "textureWrap": "Clamp"}
                  ],
                  "states": [],
                  "variants": []
                }
              }
            }
            """;

    /** Fixture record: a JSON string + a list of entry keys to verify. */
    record Fixture(String name, String json, List<String> entryKeys) {}

    static Stream<Fixture> roundtripFixtures() {
        return Stream.of(
                new Fixture("three-entry", THREE_ENTRY_JSON, List.of("cutout", "translucent", "solid")),
                new Fixture("extended-entry", EXTENDED_JSON, List.of("cutout", "translucent", "solid", "entity"))
        );
    }

    static Stream<String> threeEntryKeys() {
        return Stream.of("cutout", "translucent", "solid");
    }

    // --- Roundtrip tests (from CodecTest + RegressionTest) ---

    @ParameterizedTest
    @MethodSource("roundtripFixtures")
    @DisplayName("CODEC roundtrip: JSON → BrMaterial → JSON → BrMaterial preserves structural equality")
    void roundtrip(Fixture fixture) {
        var jsonElement = JsonParser.parseString(fixture.json());
        var jsonObject = jsonElement.getAsJsonObject();

        var firstResult = BrMaterial.CODEC.parse(JsonOps.INSTANCE, jsonObject);
        var first = firstResult.getOrThrow(false,
                msg -> new AssertionError("First decode failed: " + msg));

        var encodedResult = BrMaterial.CODEC.encodeStart(JsonOps.INSTANCE, first);
        var encoded = encodedResult.getOrThrow(false,
                msg -> new AssertionError("Re-encode failed: " + msg));

        var secondResult = BrMaterial.CODEC.parse(JsonOps.INSTANCE, encoded);
        var second = secondResult.getOrThrow(false,
                msg -> new AssertionError("Second decode failed: " + msg));

        assertEquals(first, second,
                "Roundtrip [" + fixture.name() + "]: decoded BrMaterial differs after encode→decode");

        for (String key : fixture.entryKeys()) {
            assertTrue(first.materials().containsKey(key),
                    "Material map should contain entry: " + key);
        }
    }

    // --- Detailed field verification tests (from IntegrationTest) ---

    @ParameterizedTest
    @MethodSource("threeEntryKeys")
    @DisplayName("Parse vanilla material and verify all entry field values")
    void verifyEntryFields(String entryKey) {
        var jsonElement = JsonParser.parseString(THREE_ENTRY_JSON);
        var jsonObject = jsonElement.getAsJsonObject();

        var result = BrMaterial.CODEC.parse(JsonOps.INSTANCE, jsonObject);
        var material = result.getOrThrow(false,
                msg -> new AssertionError("Parse failed: " + msg));

        var entry = material.materials().get(entryKey);
        assertNotNull(entry, "Entry should exist: " + entryKey);

        assertEquals(entryKey, entry.name());
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
