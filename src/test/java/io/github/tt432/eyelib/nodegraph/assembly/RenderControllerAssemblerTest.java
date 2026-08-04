package io.github.tt432.eyelib.nodegraph.assembly;

import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.countCode;
import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.graph;
import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.lib;
import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.node;
import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.opts;
import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.wire;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link RenderControllerAssembler} 单测：最小空图、全字段图（JSON 结构断言）、
 * INVALID_ARRAYS 错误（非法 JSON / 非对象 JSON）。
 */
class RenderControllerAssemblerTest {

    private static JsonObject entryOf(AssemblyResult r, String identifier) {
        return r.json().getAsJsonObject("render_controllers").getAsJsonObject(identifier);
    }

    @Test
    void minimalGraph() {
        GraphLibrary lib = lib(GraphKind.RENDER_CONTROLLER, graph(
                List.of(node("root", "rc.root", opts("identifier", "controller.render.test"))),
                List.of()));

        AssemblyResult r = RenderControllerAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        assertEquals("1.8.0", r.json().get("format_version").getAsString());
        JsonObject entry = entryOf(r, "controller.render.test");
        // geometry 恒输出：未连线 → 端口默认（字符串字面量走 molang 单引号形）
        assertEquals("'geometry.default'", entry.get("geometry").getAsString());
        assertFalse(entry.get("ignore_lighting").getAsBoolean());
        assertFalse(entry.has("textures"));
        assertFalse(entry.has("materials"));
        assertFalse(entry.has("part_visibility"));
        assertFalse(entry.has("arrays"));
        assertFalse(entry.has("color"));
        assertFalse(entry.has("is_hurt_color"));
        assertFalse(entry.has("on_fire_color"));
        assertFalse(entry.has("overlay_color"));
    }

    @Test
    void fullGraph() {
        GraphLibrary lib = lib(GraphKind.RENDER_CONTROLLER, graph(
                List.of(
                        node("root", "rc.root", opts(
                                "identifier", "controller.render.full",
                                "ignore_lighting", true,
                                "arrays", "{\"bones\":{\"head\":[\"a\",\"b\"]}}")),
                        node("g1", "const.string", opts("value", "geometry.custom")),
                        node("t1", "list.entry"),
                        node("tc1", "const.string", opts("value", "texture.a")),
                        node("t2", "list.entry"),
                        node("tc2", "const.string", opts("value", "texture.b")),
                        node("m1", "material.entry", opts("pattern", "*")),
                        node("rm1", "ref.material", opts("short_name", "default")),
                        node("pv1", "part_visibility.entry", opts("bone_pattern", "head*")),
                        node("pb1", "const.bool", opts("value", true)),
                        node("pv2", "part_visibility.entry", opts("bone_pattern", "leg*"),
                                opts("condition", false)),
                        // 与 pv1 同 pattern，后来者覆盖
                        node("pv3", "part_visibility.entry", opts("bone_pattern", "head*")),
                        node("pn3", "const.number", opts("value", 0.5)),
                        node("cr", "const.number", opts("value", 0.25)),
                        node("cc", "color.compose"),
                        node("oa", "const.number", opts("value", 0.5)),
                        node("co", "color.compose"),
                        node("ck", "const.color", opts("value", "#FF00FF00"))),
                List.of(
                        wire("g1", "out", "root", "geometry"),
                        wire("t1", "entry", "root", "textures"),
                        wire("tc1", "out", "t1", "value"),
                        wire("t2", "entry", "root", "textures"),
                        wire("tc2", "out", "t2", "value"),
                        wire("m1", "entry", "root", "materials"),
                        wire("rm1", "ref", "m1", "value"),
                        wire("pv1", "entry", "root", "part_visibility"),
                        wire("pb1", "out", "pv1", "condition"),
                        wire("pv2", "entry", "root", "part_visibility"),
                        wire("pv3", "entry", "root", "part_visibility"),
                        wire("pn3", "out", "pv3", "condition"),
                        // color：compose r 接表达式，g/b/a 走端口默认 1
                        wire("cr", "out", "cc", "r"),
                        wire("cc", "out", "root", "color"),
                        // overlay_color：compose a 接表达式
                        wire("oa", "out", "co", "a"),
                        wire("co", "out", "root", "overlay_color"),
                        // is_hurt_color：const.color 纯色（#FF00FF00 = a1 r0 g1 b0）
                        wire("ck", "out", "root", "is_hurt_color"))));

        AssemblyResult r = RenderControllerAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject entry = entryOf(r, "controller.render.full");

        assertEquals("'geometry.custom'", entry.get("geometry").getAsString());

        JsonArray textures = entry.getAsJsonArray("textures");
        assertEquals(2, textures.size());
        assertEquals("'texture.a'", textures.get(0).getAsString());
        assertEquals("'texture.b'", textures.get(1).getAsString());

        JsonArray materials = entry.getAsJsonArray("materials");
        assertEquals(1, materials.size());
        assertEquals("material.default", materials.get(0).getAsJsonObject().get("*").getAsString());

        // part_visibility：pv3 覆盖 pv1（head* → "0.5"，保持首现位置），pv2 内联常量 false → "0"
        JsonArray pv = entry.getAsJsonArray("part_visibility");
        assertEquals(2, pv.size());
        assertEquals("0.5", pv.get(0).getAsJsonObject().get("head*").getAsString());
        assertEquals("0", pv.get(1).getAsJsonObject().get("leg*").getAsString());

        // arrays：JSON 文本原样放入
        assertEquals(JsonParser.parseString("{\"bones\":{\"head\":[\"a\",\"b\"]}}").getAsJsonObject(),
                entry.getAsJsonObject("arrays"));

        assertTrue(entry.get("ignore_lighting").getAsBoolean());

        // color：compose 仅 r 有连线 → 组输出，其余通道取端口默认 "1"
        JsonObject color = entry.getAsJsonObject("color");
        assertEquals("0.25", color.get("r").getAsString());
        assertEquals("1", color.get("g").getAsString());
        assertEquals("1", color.get("b").getAsString());
        assertEquals("1", color.get("a").getAsString());
        // is_hurt_color：const.color → 四通道字面值
        JsonObject hurt = entry.getAsJsonObject("is_hurt_color");
        assertEquals("0", hurt.get("r").getAsString());
        assertEquals("1", hurt.get("g").getAsString());
        assertEquals("0", hurt.get("b").getAsString());
        assertEquals("1", hurt.get("a").getAsString());
        assertFalse(entry.has("on_fire_color"));
        // overlay_color：compose 仅 a 连线，其余回落 "1"
        JsonObject overlay = entry.getAsJsonObject("overlay_color");
        assertEquals("1", overlay.get("r").getAsString());
        assertEquals("1", overlay.get("g").getAsString());
        assertEquals("1", overlay.get("b").getAsString());
        assertEquals("0.5", overlay.get("a").getAsString());
    }

    @Test
    void invalidArrays() {
        GraphLibrary badSyntax = lib(GraphKind.RENDER_CONTROLLER, graph(
                List.of(node("root", "rc.root",
                        opts("identifier", "controller.render.err", "arrays", "not-json{"))),
                List.of()));
        GraphLibrary notObject = lib(GraphKind.RENDER_CONTROLLER, graph(
                List.of(node("root", "rc.root",
                        opts("identifier", "controller.render.err", "arrays", "[1,2]"))),
                List.of()));

        for (GraphLibrary lib : List.of(badSyntax, notObject)) {
            AssemblyResult r = RenderControllerAssembler.assemble(lib);
            assertTrue(r.hasErrors());
            assertEquals(1, countCode(r, AssemblySupport.INVALID_ARRAYS));
            // 尽力而为：arrays 字段省略，其余字段仍产出
            JsonObject entry = entryOf(r, "controller.render.err");
            assertFalse(entry.has("arrays"));
            assertEquals("'geometry.default'", entry.get("geometry").getAsString());
        }
    }
}
