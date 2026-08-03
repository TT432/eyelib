package io.github.tt432.eyelib.nodegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * ShortNames 派生规则（规格 nodegraph-shortname-elimination D1/D3）单测。
 */
class ShortNamesTest {

    // ---------- sanitize ----------

    @Test
    void identityForCleanIdentifiers() {
        assertEquals("geometry.oreville_ans.tgsary", ShortNames.sanitize("geometry.oreville_ans.tgsary"));
        assertEquals("entity_alphatest", ShortNames.sanitize("entity_alphatest"));
        assertEquals("default", ShortNames.sanitize("default"));
    }

    @Test
    void slashBecomesDot() {
        assertEquals("textures.oreville.ans.czm", ShortNames.sanitize("textures/oreville/ans/czm"));
    }

    @Test
    void lowercases() {
        assertEquals("geometry.foo.bar", ShortNames.sanitize("Geometry.Foo.Bar"));
    }

    @Test
    void illegalCharsBecomeUnderscore() {
        assertEquals("geometry.my_model", ShortNames.sanitize("geometry.my-model"));
        assertEquals("a_b", ShortNames.sanitize("a:b"));
    }

    @Test
    void leadingDigitSegmentGetsUnderscorePrefix() {
        assertEquals("texture._2nd", ShortNames.sanitize("texture.2nd"));
        assertEquals("_1ab", ShortNames.sanitize("1ab"));
        // 段中非首字符数字保留
        assertEquals("a.b2c", ShortNames.sanitize("a.b2c"));
    }

    @Test
    void dotsCollapsedAndTrimmed() {
        assertEquals("a.b", ShortNames.sanitize("a..b"));
        assertEquals("a.b", ShortNames.sanitize(".a.b."));
        assertEquals("a", ShortNames.sanitize("...a..."));
    }

    @Test
    void emptyStaysEmpty() {
        assertEquals("", ShortNames.sanitize(""));
    }

    // ---------- isSanitized ----------

    @Test
    void sanitizedCheck() {
        assertTrue(ShortNames.isSanitized("geometry.oreville_ans.tgsary"));
        assertFalse(ShortNames.isSanitized("Geometry.Default"));
        assertFalse(ShortNames.isSanitized("textures/a/b"));
        assertFalse(ShortNames.isSanitized("my-mat"));
    }

    // ---------- effective ----------

    private static NodeInstance ref(String type, String shortName, String valueOption, String value) {
        return new NodeInstance("n1", type, 0, 0,
                java.util.Map.of("short_name", new com.google.gson.JsonPrimitive(shortName),
                        valueOption, new com.google.gson.JsonPrimitive(value)),
                java.util.Map.of());
    }

    @Test
    void explicitOverridesDerivation() {
        NodeInstance n = ref("ref.geometry", "custom", "identifier", "geometry.a.b");
        assertEquals("custom", ShortNames.effective(n, NodeTypes.REF_GEOMETRY));
    }

    @Test
    void emptyExplicitDerives() {
        NodeInstance n = ref("ref.geometry", "", "identifier", "geometry.a.b");
        assertEquals("geometry.a.b", ShortNames.effective(n, NodeTypes.REF_GEOMETRY));

        NodeInstance tex = ref("ref.texture", "", "path", "textures/entity/skin");
        assertEquals("textures.entity.skin", ShortNames.effective(tex, NodeTypes.REF_TEXTURE));

        NodeInstance anim = ref("ref.animation", "", "identifier", "animation.test.walk");
        assertEquals("animation.test.walk", ShortNames.effective(anim, NodeTypes.REF_ANIMATION));
    }

    @Test
    void bothEmptyYieldsEmpty() {
        NodeInstance n = ref("ref.material", "", "material", "");
        assertEquals("", ShortNames.effective(n, NodeTypes.REF_MATERIAL));
    }

    // ---------- valueOptionOf / isMolangEmitted ----------

    @Test
    void categoryMappings() {
        assertEquals("identifier", ShortNames.valueOptionOf("ref.geometry"));
        assertEquals("path", ShortNames.valueOptionOf("ref.texture"));
        assertEquals("material", ShortNames.valueOptionOf("ref.material"));
        assertEquals("identifier", ShortNames.valueOptionOf("ref.animation"));
        assertEquals("identifier", ShortNames.valueOptionOf("ref.ac"));
        assertEquals(null, ShortNames.valueOptionOf("ref.rc"));
        assertEquals(null, ShortNames.valueOptionOf("const.number"));

        assertTrue(ShortNames.isMolangEmitted("ref.geometry"));
        assertTrue(ShortNames.isMolangEmitted("ref.texture"));
        assertTrue(ShortNames.isMolangEmitted("ref.material"));
        assertFalse(ShortNames.isMolangEmitted("ref.animation"));
        assertFalse(ShortNames.isMolangEmitted("ref.ac"));
    }
}
