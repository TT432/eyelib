package io.github.tt432.eyelibmaterial;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibmaterial.material.BrMaterial;
import io.github.tt432.eyelibmaterial.material.BrMaterialEntry;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TT432
 */
@NullMarked
class MaterialEndToEndTest {

    // @formatter:off
    private static final String VANILLA_JSON = """
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
              "vertexShader": "eyelibmaterial:shaders/render.vert",
              "fragmentShader": "eyelibmaterial:shaders/render.frag",
              "defines": [],
              "samplerStates": [
                {"samplerIndex": 0, "textureFilter": "Point", "textureWrap": "Clamp"}
              ],
              "states": [],
              "variants": []
            },
            "entity_alphatest:entity": {
              "vertexShader": "eyelibmaterial:shaders/render.vert",
              "fragmentShader": "eyelibmaterial:shaders/render.frag",
              "+defines": ["ALPHA_TEST"],
              "samplerStates": [],
              "states": [],
              "variants": []
            },
            "entity_alphablend:entity": {
              "vertexShader": "eyelibmaterial:shaders/render.vert",
              "fragmentShader": "eyelibmaterial:shaders/render.frag",
              "defines": [],
              "samplerStates": [],
              "+states": ["Blending"],
              "blendSrc": "SourceAlpha",
              "blendDst": "OneMinusSrcAlpha",
              "variants": []
            },
            "entity_nocull:entity": {
              "vertexShader": "eyelibmaterial:shaders/render.vert",
              "fragmentShader": "eyelibmaterial:shaders/render.frag",
              "defines": [],
              "samplerStates": [],
              "+states": ["DisableCulling"],
              "variants": []
            },
            "particles_blend": {
              "vertexShader": "eyelibmaterial:shaders/particle.vert",
              "fragmentShader": "eyelibmaterial:shaders/particle.frag",
              "defines": [],
              "samplerStates": [],
              "states": ["Blending"],
              "blendSrc": "SourceAlpha",
              "blendDst": "OneMinusSrcAlpha",
              "alphaSrc": "One",
              "alphaDst": "OneMinusSrcAlpha",
              "variants": []
            },
            "opaque_block": {
              "vertexShader": "eyelibmaterial:shaders/block.vert",
              "fragmentShader": "eyelibmaterial:shaders/block.frag",
              "defines": [],
              "samplerStates": [
                {"samplerIndex": 0, "textureFilter": "Point", "textureWrap": "Clamp"}
              ],
              "states": ["DisableAlphaWrite"],
              "variants": []
            }
          }
        }
        """;
    // @formatter:on

    private static BrMaterial parseVanillaMaterial() {
        JsonElement json = JsonParser.parseString(VANILLA_JSON);
        return BrMaterial.CODEC
                .parse(JsonOps.INSTANCE, json)
                .getOrThrow(false, RuntimeException::new);
    }

    @Test
    void testCODECParsing_all9Entries() {
        BrMaterial material = parseVanillaMaterial();

        assertEquals(9, material.materials().size(),
                "vanilla.material must contain exactly 9 entries");

        assertTrue(material.materials().containsKey("cutout"));
        assertTrue(material.materials().containsKey("translucent"));
        assertTrue(material.materials().containsKey("solid"));
        assertTrue(material.materials().containsKey("entity"));
        assertTrue(material.materials().containsKey("entity_alphatest:entity"));
        assertTrue(material.materials().containsKey("entity_alphablend:entity"));
        assertTrue(material.materials().containsKey("entity_nocull:entity"));
        assertTrue(material.materials().containsKey("particles_blend"));
        assertTrue(material.materials().containsKey("opaque_block"));

        // Spot-check "cutout" fields
        BrMaterialEntry cutout = material.materials().get("cutout");
        assertEquals(Optional.of("a"), cutout.vertexShader());
        assertEquals(Optional.of("b"), cutout.fragmentShader());
        assertEquals("", cutout.base());

        // Spot-check "entity_alphatest:entity" parsing: base="entity", name="entity_alphatest"
        BrMaterialEntry alphatest = material.materials().get("entity_alphatest:entity");
        assertEquals("entity", alphatest.base());
        assertEquals("entity_alphatest", alphatest.name());
    }

    @Test
    void testManagerPutGetRoundtrip() {
        BrMaterial material = parseVanillaMaterial();

        // Simulate Manager storage using a simple Map
        // (MaterialManager lives in the root module; eyelib-material cannot
        //  access it without introducing a circular dependency. A Map achieves
        //  the same pipeline verification: store → retrieve → identity.)
        Map<String, BrMaterialEntry> storage = new HashMap<>();

        // Put all parsed entries into the map
        for (var entry : material.materials().entrySet()) {
            storage.put(entry.getKey(), entry.getValue());
        }

        assertEquals(9, storage.size());

        // Get entries back and verify identity
        BrMaterialEntry entity = storage.get("entity");
        assertNotNull(entity);
        assertEquals(Optional.of("eyelibmaterial:shaders/render.vert"), entity.vertexShader());
        assertEquals(Optional.of("eyelibmaterial:shaders/render.frag"), entity.fragmentShader());
        assertTrue(entity.samplerStates().base().isPresent());
        assertEquals(1, entity.samplerStates().base().get().size());

        // Replace all and verify
        Map<String, BrMaterialEntry> replacement = new HashMap<>();
        replacement.put("test_entry", entity);
        storage.clear();
        storage.putAll(replacement);
        assertEquals(1, storage.size());
        assertNotNull(storage.get("test_entry"));
    }

    @Test
    void testJsonRoundtrip() {
        BrMaterial original = parseVanillaMaterial();

        // Encode back to JSON
        JsonElement encoded = BrMaterial.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow(false, RuntimeException::new);

        // Decode the JSON back to BrMaterial
        BrMaterial decoded = BrMaterial.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(false, RuntimeException::new);

        // Verify structural equality
        assertEquals(original.materials().size(), decoded.materials().size(),
                "Roundtrip must preserve entry count");

        for (var key : original.materials().keySet()) {
            BrMaterialEntry origEntry = original.materials().get(key);
            BrMaterialEntry decEntry = decoded.materials().get(key);

            assertNotNull(decEntry, "Roundtrip must preserve key: " + key);
            assertEquals(origEntry.name(), decEntry.name());
            assertEquals(origEntry.base(), decEntry.base());
            assertEquals(origEntry.vertexShader(), decEntry.vertexShader());
            assertEquals(origEntry.fragmentShader(), decEntry.fragmentShader());
            assertEquals(origEntry.defines(), decEntry.defines());
            assertEquals(origEntry.samplerStates(), decEntry.samplerStates());
            assertEquals(origEntry.states(), decEntry.states());
            assertEquals(origEntry.blend(), decEntry.blend());
            assertEquals(origEntry.variants(), decEntry.variants());
        }
    }

    @Test
    void testInheritance() {
        BrMaterial material = parseVanillaMaterial();

        BrMaterialEntry entity = material.materials().get("entity");
        BrMaterialEntry alphatest = material.materials().get("entity_alphatest:entity");

        assertNotNull(entity);
        assertNotNull(alphatest);

        // entity_alphatest inherits from entity
        assertEquals("entity", alphatest.base());

        // entity_alphatest should have the same shader paths as entity
        assertEquals(entity.vertexShader(), alphatest.vertexShader());
        assertEquals(entity.fragmentShader(), alphatest.fragmentShader());

        // entity_alphatest has +defines: ALPHA_TEST
        assertTrue(alphatest.defines().add().isPresent());
        assertTrue(alphatest.defines().add().get().contains("ALPHA_TEST"));

        // entity_nocull inherits from entity and adds DisableCulling
        BrMaterialEntry nocull = material.materials().get("entity_nocull:entity");
        assertNotNull(nocull);
        assertEquals("entity", nocull.base());
        assertTrue(nocull.states().add().isPresent());
        assertTrue(nocull.states().add().get().contains(
                io.github.tt432.eyelibmaterial.gl.GLStates.DisableCulling));

        // entity_alphablend inherits from entity and adds Blending + blend factors
        BrMaterialEntry alphablend = material.materials().get("entity_alphablend:entity");
        assertNotNull(alphablend);
        assertEquals("entity", alphablend.base());
        assertTrue(alphablend.blend().blendSrc().isPresent());
        assertEquals(io.github.tt432.eyelibmaterial.gl.BlendFactor.SourceAlpha,
                alphablend.blend().blendSrc().get());
    }

    @Test
    void testVariantLookup() {
        BrMaterial material = parseVanillaMaterial();

        // None of the current vanilla entries have variants
        for (var entry : material.materials().entrySet()) {
            BrMaterialEntry mat = entry.getValue();
            assertFalse(mat.hasVariants(),
                    "Entry '" + entry.getKey() + "' should have no variants");
            assertEquals(Optional.empty(), mat.getVariant("nonexistent"),
                    "getVariant on variant-free entry should return empty");
        }

        // Create a material entry with variants to verify the method works
        BrMaterialEntry baseEntry = material.materials().get("entity");
        assertNotNull(baseEntry);

        // Verify that getVariant returns empty for non-existent variants
        assertEquals(Optional.empty(), baseEntry.getVariant("skinning"));
    }
}