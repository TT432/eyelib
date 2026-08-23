package io.github.tt432.eyelib.importer.addon;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BrUiFile#parse}：namespace 提取、顶层元素表保留（含 {@code @} 继承 key 原样保留）、
 * 非对象顶层值忽略、有效命名空间回落文件名。
 *
 * @author TT432
 */
class BrUiFileTest {

    @Test
    void parsesNamespaceAndElements() {
        BrUiFile file = BrUiFile.parse(JsonParser.parseString("""
                {
                  "namespace": "hud",
                  "title_label": { "type": "label", "text": "hello" },
                  "root_panel": { "type": "panel" }
                }
                """).getAsJsonObject());

        assertEquals("hud", file.namespace());
        assertEquals(List.of("title_label", "root_panel"), List.copyOf(file.elements().keySet()));
        assertEquals("label", file.elements().get("title_label").get("type").getAsString());
    }

    @Test
    void keepsInheritanceKeyShapeUnexpanded() {
        BrUiFile file = BrUiFile.parse(JsonParser.parseString("""
                {
                  "namespace": "hud",
                  "my_button@common.button": { "color": [1, 0, 0] }
                }
                """).getAsJsonObject());

        assertTrue(file.elements().containsKey("my_button@common.button"));
        assertEquals("my_button", BrUiFile.namePart("my_button@common.button"));
        assertEquals("plain", BrUiFile.namePart("plain"));
    }

    @Test
    void missingNamespaceFallsBackToFileName() {
        BrUiFile file = BrUiFile.parse(JsonParser.parseString("""
                { "root": { "type": "panel" } }
                """).getAsJsonObject());

        assertNull(file.namespace());
        assertEquals("hud_screen", file.effectiveNamespace("ui/hud_screen.json"));
        assertEquals("hud_screen", file.effectiveNamespace("ui\\hud_screen.json"));
    }

    @Test
    void explicitNamespaceWinsOverFileName() {
        BrUiFile file = BrUiFile.parse(JsonParser.parseString("""
                { "namespace": "custom_ns", "root": {} }
                """).getAsJsonObject());

        assertEquals("custom_ns", file.effectiveNamespace("ui/hud_screen.json"));
    }

    @Test
    void ignoresNonObjectTopLevelValues() {
        BrUiFile file = BrUiFile.parse(JsonParser.parseString("""
                {
                  "namespace": "hud",
                  "some_string": "not a control",
                  "some_number": 42,
                  "real_control": { "type": "panel" }
                }
                """).getAsJsonObject());

        assertEquals(List.of("real_control"), List.copyOf(file.elements().keySet()));
    }
}
