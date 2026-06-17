package io.github.tt432.eyelib.material;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.material.material.BrMaterial;
import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TT432
 */
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
            "entity_change_color:entity_nocull": {
              "vertexShader": "eyelibmaterial:shaders/render.vert",
              "fragmentShader": "eyelibmaterial:shaders/render.frag",
              "+defines": ["USE_OVERLAY", "USE_COLOR_MASK"],
              "samplerStates": [],
              "states": [],
              "variants": []
            },
            "entity_alphatest_change_color:entity_change_color": {
              "vertexShader": "eyelibmaterial:shaders/render.vert",
              "fragmentShader": "eyelibmaterial:shaders/render.frag",
              "+defines": ["ALPHA_TEST", "USE_COLOR_MASK"],
              "samplerStates": [
                {"samplerIndex": 1, "textureWrap": "Repeat"}
              ],
              "+states": ["DisableAlphaWrite"],
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
    void testCODECParsing_all11Entries() {
        BrMaterial material = parseVanillaMaterial();

        assertEquals(11, material.materials().size(),
                     "vanilla.material must contain exactly 11 entries");

        assertTrue(material.materials().containsKey("cutout"));
        assertTrue(material.materials().containsKey("translucent"));
        assertTrue(material.materials().containsKey("solid"));
        assertTrue(material.materials().containsKey("entity"));
        assertTrue(material.materials().containsKey("entity_alphatest:entity"));
        assertTrue(material.materials().containsKey("entity_alphablend:entity"));
        assertTrue(material.materials().containsKey("entity_nocull:entity"));
        assertTrue(material.materials().containsKey("entity_change_color:entity_nocull"));
        assertTrue(material.materials().containsKey("entity_alphatest_change_color:entity_change_color"));
        assertTrue(material.materials().containsKey("particles_blend"));
        assertTrue(material.materials().containsKey("opaque_block"));

        // 抽查 "cutout" 字段
        BrMaterialEntry cutout = material.materials().get("cutout");
        assertEquals(Optional.of("a"), cutout.vertexShader());
        assertEquals(Optional.of("b"), cutout.fragmentShader());
        assertEquals("", cutout.base());

        // 抽查 "entity_alphatest:entity" 解析: base="entity", name="entity_alphatest"
        BrMaterialEntry alphatest = material.materials().get("entity_alphatest:entity");
        assertEquals("entity", alphatest.base());
        assertEquals("entity_alphatest", alphatest.name());
    }

    @Test
    void testJsonRoundtrip() {
        BrMaterial original = parseVanillaMaterial();

        // 编码回 JSON
        JsonElement encoded = BrMaterial.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow(false, RuntimeException::new);

        // 再解码 JSON 回 BrMaterial
        BrMaterial decoded = BrMaterial.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(false, RuntimeException::new);

        // 验证结构相等性
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
    void testInheritance_alphatestInheritsFromEntity() {
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
    }

    @Test
    void testInheritance_nocullInheritsFromEntity() {
        BrMaterial material = parseVanillaMaterial();

        BrMaterialEntry nocull = material.materials().get("entity_nocull:entity");
        assertNotNull(nocull);
        assertEquals("entity", nocull.base());
        assertTrue(nocull.states().add().isPresent());
        assertTrue(nocull.states().add().get().contains(
                io.github.tt432.eyelib.material.gl.GLStates.DisableCulling));
    }

    @Test
    void testInheritance_alphablendInheritsFromEntity() {
        BrMaterial material = parseVanillaMaterial();

        BrMaterialEntry alphablend = material.materials().get("entity_alphablend:entity");
        assertNotNull(alphablend);
        assertEquals("entity", alphablend.base());
        assertTrue(alphablend.blend().blendSrc().isPresent());
        assertEquals(io.github.tt432.eyelib.material.gl.BlendFactor.SourceAlpha,
                     alphablend.blend().blendSrc().get());
    }

    @Test
    void testInheritance_particlesBlendStandalone() {
        BrMaterial material = parseVanillaMaterial();

        BrMaterialEntry particlesBlend = material.materials().get("particles_blend");
        assertNotNull(particlesBlend);
        assertEquals("", particlesBlend.base());
        assertTrue(particlesBlend.states().base().isPresent());
        assertTrue(particlesBlend.states().base().get().contains(
                io.github.tt432.eyelib.material.gl.GLStates.Blending));
        assertEquals(io.github.tt432.eyelib.material.gl.BlendFactor.SourceAlpha,
                     particlesBlend.blend().blendSrc().get());
        assertEquals(io.github.tt432.eyelib.material.gl.BlendFactor.OneMinusSrcAlpha,
                     particlesBlend.blend().blendDst().get());
    }

    @Test
    void testVariantLookup() {
        BrMaterial material = parseVanillaMaterial();

        // 当前所有 vanilla 条目均无变体
        for (var entry : material.materials().entrySet()) {
            BrMaterialEntry mat = entry.getValue();
            assertFalse(mat.hasVariants(),
                        "Entry '" + entry.getKey() + "' should have no variants");
            assertEquals(Optional.empty(), mat.getVariant("nonexistent"),
                         "getVariant on variant-free entry should return empty");
        }

        // 创建带变体的材质条目以验证方法工作
        BrMaterialEntry baseEntry = material.materials().get("entity");
        assertNotNull(baseEntry);

        // 验证变体不存在时返回空
        assertEquals(Optional.empty(), baseEntry.getVariant("skinning"));
    }
}
