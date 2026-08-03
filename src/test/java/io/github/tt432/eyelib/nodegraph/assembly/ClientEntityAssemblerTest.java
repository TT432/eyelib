package io.github.tt432.eyelib.nodegraph.assembly;

import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.countCode;
import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.graph;
import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.hasCode;
import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.lib;
import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.node;
import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.opts;
import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.wire;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphInterface;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.PortType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * {@link ClientEntityAssembler} 单测：最小空图、全字段图（JSON 结构断言）、
 * MISSING_ENTRY_REF 错误、声明表去重与子图可达收集。
 */
class ClientEntityAssemblerTest {

    @Test
    void minimalGraph() {
        GraphLibrary lib = lib(GraphKind.CLIENT_ENTITY, graph(
                List.of(node("root", "entity.root", opts("identifier", "test:dummy"))),
                List.of()));

        AssemblyResult r = ClientEntityAssembler.assemble(lib);

        assertFalse(r.hasErrors());
        assertEquals("1.10.0", r.json().get("format_version").getAsString());
        JsonObject desc = r.json().getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
        assertEquals("test:dummy", desc.get("identifier").getAsString());
        assertFalse(desc.has("scripts"));
        assertFalse(desc.has("geometry"));
        assertFalse(desc.has("textures"));
        assertFalse(desc.has("materials"));
        assertFalse(desc.has("animations"));
        assertFalse(desc.has("animation_controllers"));
        assertFalse(desc.has("render_controllers"));
    }

    @Test
    void fullGraph() {
        GraphLibrary lib = lib(GraphKind.CLIENT_ENTITY, graph(
                List.of(
                        node("root", "entity.root", opts("identifier", "test:full"),
                                opts("scale_x", 2)),
                        node("s1", "exec.set_var", opts("name", "variable.foo")),
                        node("cscale", "const.number", opts("value", 1.5)),
                        node("ae1", "animate.entry"),
                        node("w1", "const.number", opts("value", 0.5)),
                        node("ra1", "ref.animation",
                                opts("short_name", "walk", "identifier", "animation.test.walk")),
                        node("ae2", "animate.entry"),
                        node("rac1", "ref.ac",
                                opts("short_name", "main", "identifier", "controller.animation.test.main")),
                        node("rce1", "rc.condition_entry"),
                        node("rrc1", "ref.rc", opts("identifier", "controller.render.test.a")),
                        node("rce2", "rc.condition_entry"),
                        node("rrc2", "ref.rc", opts("identifier", "controller.render.test.b")),
                        node("q1", "query.call", opts("function", "query.is_baby", "arg_count", 0)),
                        node("rg1", "ref.geometry",
                                opts("short_name", "default", "identifier", "geometry.test.model")),
                        node("rt1", "ref.texture",
                                opts("short_name", "default", "path", "textures/entity/test")),
                        node("rm1", "ref.material",
                                opts("short_name", "default", "material", "entity_alphatest"))),
                List.of(
                        wire("s1", "exec_out", "root", "initialize"),
                        wire("cscale", "out", "root", "scale"),
                        wire("ae1", "entry", "root", "animate"),
                        wire("w1", "out", "ae1", "weight"),
                        wire("ra1", "ref", "ae1", "ref"),
                        wire("ae2", "entry", "root", "animate"),
                        wire("rac1", "ref", "ae2", "ref"),
                        wire("rce1", "entry", "root", "render_controllers"),
                        wire("rrc1", "ref", "rce1", "rc"),
                        wire("rce2", "entry", "root", "render_controllers"),
                        wire("rrc2", "ref", "rce2", "rc"),
                        wire("q1", "out", "rce2", "condition"))));

        AssemblyResult r = ClientEntityAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject desc = r.json().getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
        assertEquals("test:full", desc.get("identifier").getAsString());

        // scripts
        JsonObject scripts = desc.getAsJsonObject("scripts");
        assertEquals("variable.foo = 0", scripts.get("initialize").getAsString());
        assertFalse(scripts.has("pre_animation"));
        assertFalse(scripts.has("parent_setup"));
        assertEquals("1.5", scripts.get("scale").getAsString());
        assertEquals("2", scripts.get("scaleX").getAsString());
        assertFalse(scripts.has("scaleY"));
        assertFalse(scripts.has("scaleZ"));

        // scripts.animate：uid 字典序 ae1 < ae2；weight 恒输出（ae2 未连线 → 默认 "1"）
        JsonArray animate = scripts.getAsJsonArray("animate");
        assertEquals(2, animate.size());
        assertEquals("0.5", animate.get(0).getAsJsonObject().get("walk").getAsString());
        assertEquals("1", animate.get(1).getAsJsonObject().get("main").getAsString());

        // 声明表
        assertEquals("geometry.test.model",
                desc.getAsJsonObject("geometry").get("default").getAsString());
        assertEquals("textures/entity/test",
                desc.getAsJsonObject("textures").get("default").getAsString());
        assertEquals("entity_alphatest",
                desc.getAsJsonObject("materials").get("default").getAsString());
        assertEquals("animation.test.walk",
                desc.getAsJsonObject("animations").get("walk").getAsString());
        // D6：ref.ac 与 ref.animation 同发进 animations 表；animation_controllers 不再发射
        assertEquals("controller.animation.test.main",
                desc.getAsJsonObject("animations").get("main").getAsString());
        assertFalse(desc.has("animation_controllers"));

        // render_controllers：condition 恒 "1" → 纯字符串；否则 {identifier: condition}
        JsonArray rcs = desc.getAsJsonArray("render_controllers");
        assertEquals(2, rcs.size());
        assertEquals("controller.render.test.a", rcs.get(0).getAsString());
        assertEquals("query.is_baby",
                rcs.get(1).getAsJsonObject().get("controller.render.test.b").getAsString());
    }

    @Test
    void missingEntryRef() {
        GraphLibrary lib = lib(GraphKind.CLIENT_ENTITY, graph(
                List.of(
                        node("root", "entity.root", opts("identifier", "test:err")),
                        node("ae1", "animate.entry")),
                List.of(wire("ae1", "entry", "root", "animate"))));

        AssemblyResult r = ClientEntityAssembler.assemble(lib);

        assertTrue(r.hasErrors());
        assertEquals(1, countCode(r, AssemblySupport.MISSING_ENTRY_REF));
        // 尽力而为：JSON 仍产出；无法确定 short_name 的条目被跳过 → scripts 整体省略
        JsonObject desc = r.json().getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
        assertEquals("test:err", desc.get("identifier").getAsString());
        assertFalse(desc.has("scripts"));
    }

    @Test
    void declarationTablesDedupAndSubgraphReachable() {
        GraphInterface iface = new GraphInterface(List.of(),
                new GraphInterface.Param("result", PortType.FLOAT, Optional.empty()));
        GraphData main = graph(
                List.of(
                        node("root", "entity.root", opts("identifier", "test:sub")),
                        node("sc1", "subgraph.call", opts("subgraph", "sub")),
                        node("rg1", "ref.geometry",
                                opts("short_name", "default", "identifier", "geometry.test.model"))),
                List.of());
        GraphData sub = new GraphData(
                List.of(
                        node("rg2", "ref.geometry",
                                opts("short_name", "sub_geo", "identifier", "geometry.test.sub")),
                        // 与 rg1 同 short_name + 同 identifier → 去重
                        node("rg3", "ref.geometry",
                                opts("short_name", "default", "identifier", "geometry.test.model"))),
                List.of(), List.of(), List.of(), List.of(), Optional.of(iface));
        GraphLibrary lib = new GraphLibrary(1, GraphKind.CLIENT_ENTITY, "root",
                Map.of("root", main, "sub", sub));

        AssemblyResult r = ClientEntityAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject desc = r.json().getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
        JsonObject geometry = desc.getAsJsonObject("geometry");
        assertEquals(2, geometry.entrySet().size());
        assertEquals("geometry.test.model", geometry.get("default").getAsString());
        assertEquals("geometry.test.sub", geometry.get("sub_geo").getAsString());
        assertFalse(hasCode(r, AssemblySupport.INVALID_ENTRY_REF));
    }

    // ---------- 短名派生与 default 别名（规格 D1/D2/D6） ----------

    @Test
    void derivedShortNamesAndDefaultAlias() {
        // 无显式 short_name → 表键为派生名；单资产表自动补 default 别名
        GraphLibrary lib = lib(GraphKind.CLIENT_ENTITY, graph(
                List.of(
                        node("root", "entity.root", opts("identifier", "test:derived")),
                        node("rg1", "ref.geometry", opts("identifier", "geometry.test.model")),
                        node("rt1", "ref.texture", opts("path", "textures/entity/test")),
                        node("rm1", "ref.material", opts("material", "entity_alphatest")),
                        node("ra1", "ref.animation", opts("identifier", "animation.test.walk")),
                        node("rac1", "ref.ac", opts("identifier", "controller.animation.test.main"))),
                List.of()));

        AssemblyResult r = ClientEntityAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject desc = r.json().getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
        JsonObject geometry = desc.getAsJsonObject("geometry");
        assertEquals("geometry.test.model", geometry.get("geometry.test.model").getAsString());
        // D2：单资产 → default 别名
        assertEquals("geometry.test.model", geometry.get("default").getAsString());
        assertEquals(2, geometry.entrySet().size());
        JsonObject textures = desc.getAsJsonObject("textures");
        assertEquals("textures/entity/test", textures.get("textures.entity.test").getAsString());
        assertEquals("textures/entity/test", textures.get("default").getAsString());
        JsonObject materials = desc.getAsJsonObject("materials");
        assertEquals("entity_alphatest", materials.get("entity_alphatest").getAsString());
        assertEquals("entity_alphatest", materials.get("default").getAsString());
        // D6：ref.ac 进 animations 表；动画表不发 default 别名
        JsonObject animations = desc.getAsJsonObject("animations");
        assertEquals("animation.test.walk", animations.get("animation.test.walk").getAsString());
        assertEquals("controller.animation.test.main", animations.get("controller.animation.test.main").getAsString());
        assertFalse(animations.has("default"));
        assertFalse(desc.has("animation_controllers"));
    }

    @Test
    void noDefaultAliasForMultiAssetTable() {
        // 多资产表不发 default 别名（歧义）
        GraphLibrary lib = lib(GraphKind.CLIENT_ENTITY, graph(
                List.of(
                        node("root", "entity.root", opts("identifier", "test:multi")),
                        node("rg1", "ref.geometry", opts("identifier", "geometry.test.a")),
                        node("rg2", "ref.geometry", opts("identifier", "geometry.test.b"))),
                List.of()));

        AssemblyResult r = ClientEntityAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject desc = r.json().getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
        JsonObject geometry = desc.getAsJsonObject("geometry");
        assertEquals(2, geometry.entrySet().size());
        assertFalse(geometry.has("default"));
    }

    @Test
    void explicitDefaultSuppressesAlias() {
        // 显式 default 存在时不再补别名；另一资产照常派生
        GraphLibrary lib = lib(GraphKind.CLIENT_ENTITY, graph(
                List.of(
                        node("root", "entity.root", opts("identifier", "test:explicit")),
                        node("rg1", "ref.geometry",
                                opts("short_name", "default", "identifier", "geometry.test.main")),
                        node("rg2", "ref.geometry", opts("identifier", "geometry.test.alt"))),
                List.of()));

        AssemblyResult r = ClientEntityAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject geometry = r.json().getAsJsonObject("minecraft:client_entity")
                .getAsJsonObject("description").getAsJsonObject("geometry");
        assertEquals(2, geometry.entrySet().size());
        assertEquals("geometry.test.main", geometry.get("default").getAsString());
        assertEquals("geometry.test.alt", geometry.get("geometry.test.alt").getAsString());
    }
}
