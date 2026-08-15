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
 * MISSING_ENTRY_REF 错误、声明表连线收集与去重、声明端口类型不匹配。
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
                        // v13：initialize 链挂 event.initialize 事件源节点
                        node("ev1", "event.initialize"),
                        node("s1", "exec.set_var"),
                        node("s1t", "variable", opts("name", "foo")),
                        node("cscale", "const.number", opts("value", 1.5)),
                        node("ae1", "animate.entry"),
                        node("w1", "const.number", opts("value", 0.5)),
                        node("ra1", "ref.animation",
                                opts("short_name", "walk", "identifier", "animation.test.walk")),
                        node("ae2", "animate.entry"),
                        node("rac1", "ref.ac",
                                opts("short_name", "main", "identifier", "controller.animation.test.main")),
                        node("rrc1", "ref.rc", opts("identifier", "controller.render.test.a")),
                        node("rrc2", "ref.rc", opts("identifier", "controller.render.test.b")),
                        node("q1", "query.call", opts("function", "query.is_baby", "arg_count", 0)),
                        node("rg1", "ref.geometry",
                                opts("short_name", "default", "identifier", "geometry.test.model")),
                        node("rt1", "ref.texture",
                                opts("short_name", "default", "path", "textures/entity/test")),
                        node("rm1", "ref.material",
                                opts("short_name", "default", "material", "entity_alphatest"))),
                List.of(
                        // v13：event.initialize.exec_out → 链首 exec_in；链尾 exec_out 悬空
                        wire("ev1", "exec_out", "s1", "exec_in"),
                        wire("s1", "target", "s1t", "in"),
                        wire("cscale", "out", "root", "scale"),
                        wire("ae1", "entry", "root", "animate"),
                        wire("w1", "out", "ae1", "weight"),
                        wire("ra1", "ref", "ae1", "ref"),
                        wire("ae2", "entry", "root", "animate"),
                        wire("rac1", "ref", "ae2", "ref"),
                        // 声明连线（v4：geo/tex/mat 锚点在 RC，动画/AC 保留实体级）
                        wire("rg1", "ref", "rrc1", "decl_geometries"),
                        wire("rt1", "ref", "rrc1", "decl_textures"),
                        wire("rm1", "ref", "rrc1", "decl_materials"),
                        wire("ra1", "ref", "root", "animations"),
                        wire("rac1", "ref", "root", "animation_controllers"),
                        // v4：ref.rc 直连 render_controllers；条件在 ref.rc.condition 端口
                        wire("rrc1", "ref", "root", "render_controllers"),
                        wire("rrc2", "ref", "root", "render_controllers"),
                        wire("q1", "out", "rrc2", "condition"))));

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
        // 无内联 rc.root → extraDocs 为空
        assertTrue(r.extraDocs().isEmpty());
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
    void declarationTablesWiredOnlyAndDedup() {
        // v4：geo/tex/mat 声明表 = DeclarationTables 从 RC 锚点派生——只有接入锚点的 ref 进表；
        // 未连线的主图 ref 与子图 ref（即使子图可达）均不收集
        GraphInterface iface = new GraphInterface(List.of(),
                new GraphInterface.Param("result", PortType.FLOAT, Optional.empty()));
        GraphData main = graph(
                List.of(
                        node("root", "entity.root", opts("identifier", "test:wired")),
                        node("sc1", "subgraph.call", opts("subgraph", "sub")),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.test.a")),
                        node("rg1", "ref.geometry",
                                opts("short_name", "default", "identifier", "geometry.test.model")),
                        // 与 rg1 同 short_name + 同 identifier → 去重
                        node("rg3", "ref.geometry",
                                opts("short_name", "default", "identifier", "geometry.test.model")),
                        node("rg9", "ref.geometry",
                                opts("short_name", "stray", "identifier", "geometry.test.stray"))),
                List.of(
                        wire("rc1", "ref", "root", "render_controllers"),
                        wire("rg1", "ref", "rc1", "decl_geometries"),
                        wire("rg3", "ref", "rc1", "decl_geometries")));
        GraphData sub = new GraphData(
                List.of(
                        node("rg2", "ref.geometry",
                                opts("short_name", "sub_geo", "identifier", "geometry.test.sub"))),
                List.of(), List.of(), List.of(), List.of(), Optional.of(iface));
        GraphLibrary lib = new GraphLibrary(1, GraphKind.CLIENT_ENTITY, "root",
                Map.of("root", main, "sub", sub));

        AssemblyResult r = ClientEntityAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject desc = r.json().getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
        JsonObject geometry = desc.getAsJsonObject("geometry");
        assertEquals(1, geometry.entrySet().size());
        assertEquals("geometry.test.model", geometry.get("default").getAsString());
    }

    @Test
    void invalidDeclarationRefType() {
        // 声明端口源节点类别不匹配 → INVALID_DECLARATION_REF（带 nodeUid），该节点不进表
        GraphLibrary lib = lib(GraphKind.CLIENT_ENTITY, graph(
                List.of(
                        node("root", "entity.root", opts("identifier", "test:badref")),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.test.a")),
                        node("rt1", "ref.texture",
                                opts("short_name", "default", "path", "textures/entity/test")),
                        node("rg1", "ref.geometry",
                                opts("short_name", "default", "identifier", "geometry.test.model"))),
                List.of(
                        wire("rc1", "ref", "root", "render_controllers"),
                        wire("rt1", "ref", "rc1", "decl_geometries"),
                        wire("rg1", "ref", "rc1", "decl_geometries"))));

        AssemblyResult r = ClientEntityAssembler.assemble(lib);

        assertTrue(r.hasErrors());
        assertEquals(1, countCode(r, AssemblySupport.INVALID_DECLARATION_REF));
        assertEquals("rt1", r.diagnostics().stream()
                .filter(d -> d.code().equals(AssemblySupport.INVALID_DECLARATION_REF))
                .findFirst().orElseThrow().nodeUid().orElseThrow());
        // 不匹配的跳过；匹配的 rg1 照常入表
        JsonObject geometry = r.json().getAsJsonObject("minecraft:client_entity")
                .getAsJsonObject("description").getAsJsonObject("geometry");
        assertEquals(1, geometry.entrySet().size());
        assertEquals("geometry.test.model", geometry.get("default").getAsString());
    }

    // ---------- 短名派生与 default 别名（规格 D1/D2/D6） ----------

    @Test
    void derivedShortNamesAndDefaultAlias() {
        // 无显式 short_name → 表键为派生名；单资产表自动补 default 别名
        GraphLibrary lib = lib(GraphKind.CLIENT_ENTITY, graph(
                List.of(
                        node("root", "entity.root", opts("identifier", "test:derived")),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.test.a")),
                        node("rg1", "ref.geometry", opts("identifier", "geometry.test.model")),
                        node("rt1", "ref.texture", opts("path", "textures/entity/test")),
                        node("rm1", "ref.material", opts("material", "entity_alphatest")),
                        node("ra1", "ref.animation", opts("identifier", "animation.test.walk")),
                        node("rac1", "ref.ac", opts("identifier", "controller.animation.test.main"))),
                List.of(
                        wire("rc1", "ref", "root", "render_controllers"),
                        wire("rg1", "ref", "rc1", "decl_geometries"),
                        wire("rt1", "ref", "rc1", "decl_textures"),
                        wire("rm1", "ref", "rc1", "decl_materials"),
                        wire("ra1", "ref", "root", "animations"),
                        wire("rac1", "ref", "root", "animation_controllers"))));

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
                        node("rc1", "ref.rc", opts("identifier", "controller.render.test.a")),
                        node("rg1", "ref.geometry", opts("identifier", "geometry.test.a")),
                        node("rg2", "ref.geometry", opts("identifier", "geometry.test.b"))),
                List.of(
                        wire("rc1", "ref", "root", "render_controllers"),
                        wire("rg1", "ref", "rc1", "decl_geometries"),
                        wire("rg2", "ref", "rc1", "decl_geometries"))));

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
                        node("rc1", "ref.rc", opts("identifier", "controller.render.test.a")),
                        node("rg1", "ref.geometry",
                                opts("short_name", "default", "identifier", "geometry.test.main")),
                        node("rg2", "ref.geometry", opts("identifier", "geometry.test.alt"))),
                List.of(
                        wire("rc1", "ref", "root", "render_controllers"),
                        wire("rg1", "ref", "rc1", "decl_geometries"),
                        wire("rg2", "ref", "rc1", "decl_geometries"))));

        AssemblyResult r = ClientEntityAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject geometry = r.json().getAsJsonObject("minecraft:client_entity")
                .getAsJsonObject("description").getAsJsonObject("geometry");
        assertEquals(2, geometry.entrySet().size());
        assertEquals("geometry.test.main", geometry.get("default").getAsString());
        assertEquals("geometry.test.alt", geometry.get("geometry.test.alt").getAsString());
    }

    // ---------- 内联 rc.root（v4 规格 §3.2/§3.3） ----------

    @Test
    void inlineRcRootProducesMergedExtraDoc() {
        // rc.root.controller 接 render_controllers → 内联 RC：列表纯字符串 + extraDocs 合并 RC 文档；
        // geometry 字段端口上游 ref 同时入声明表（声明+引用一体）
        GraphLibrary lib = lib(GraphKind.CLIENT_ENTITY, graph(
                List.of(
                        node("root", "entity.root", opts("identifier", "test:inline")),
                        node("rcr1", "rc.root", opts("identifier", "controller.render.test.a")),
                        node("rg1", "ref.geometry",
                                opts("short_name", "default", "identifier", "geometry.test.model"))),
                List.of(
                        wire("rcr1", "controller", "root", "render_controllers"),
                        wire("rg1", "ref", "rcr1", "geometry"))));

        AssemblyResult r = ClientEntityAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject desc = r.json().getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
        JsonArray rcs = desc.getAsJsonArray("render_controllers");
        assertEquals(1, rcs.size());
        assertEquals("controller.render.test.a", rcs.get(0).getAsString());
        assertEquals("geometry.test.model",
                desc.getAsJsonObject("geometry").get("default").getAsString());

        assertEquals(1, r.extraDocs().size());
        JsonObject doc = r.extraDocs().get(0);
        assertEquals("1.8.0", doc.get("format_version").getAsString());
        JsonObject entry = doc.getAsJsonObject("render_controllers")
                .getAsJsonObject("controller.render.test.a");
        assertEquals("geometry.default", entry.get("geometry").getAsString());
    }

    @Test
    void inlineRcRootWithCondition() {
        // condition 非恒 1 → {identifier: condition}；extraDocs 仍产 RC 文档
        GraphLibrary lib = lib(GraphKind.CLIENT_ENTITY, graph(
                List.of(
                        node("root", "entity.root", opts("identifier", "test:inlinecond")),
                        node("rcr1", "rc.root", opts("identifier", "controller.render.test.a")),
                        node("q1", "query.call", opts("function", "query.is_baby", "arg_count", 0))),
                List.of(
                        wire("rcr1", "controller", "root", "render_controllers"),
                        wire("q1", "out", "rcr1", "condition"))));

        AssemblyResult r = ClientEntityAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject desc = r.json().getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
        JsonArray rcs = desc.getAsJsonArray("render_controllers");
        assertEquals(1, rcs.size());
        assertEquals("query.is_baby",
                rcs.get(0).getAsJsonObject().get("controller.render.test.a").getAsString());
        assertEquals(1, r.extraDocs().size());
        assertTrue(r.extraDocs().get(0).getAsJsonObject("render_controllers")
                .has("controller.render.test.a"));
    }

    @Test
    void multipleInlineRcRootsMergeIntoSingleDoc() {
        // 多个内联 rc.root → 单个合并 render_controllers 文档；列表条目按连线源 uid 字典序
        GraphLibrary lib = lib(GraphKind.CLIENT_ENTITY, graph(
                List.of(
                        node("root", "entity.root", opts("identifier", "test:multiinline")),
                        node("rcr2", "rc.root", opts("identifier", "controller.render.test.b")),
                        node("rcr1", "rc.root", opts("identifier", "controller.render.test.a"))),
                List.of(
                        wire("rcr1", "controller", "root", "render_controllers"),
                        wire("rcr2", "controller", "root", "render_controllers"))));

        AssemblyResult r = ClientEntityAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject desc = r.json().getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
        JsonArray rcs = desc.getAsJsonArray("render_controllers");
        assertEquals(2, rcs.size());
        assertEquals("controller.render.test.a", rcs.get(0).getAsString());
        assertEquals("controller.render.test.b", rcs.get(1).getAsString());

        assertEquals(1, r.extraDocs().size());
        JsonObject controllers = r.extraDocs().get(0).getAsJsonObject("render_controllers");
        assertEquals(2, controllers.entrySet().size());
        assertTrue(controllers.has("controller.render.test.a"));
        assertTrue(controllers.has("controller.render.test.b"));
    }

    // ---------- 变量声明默认值 → initialize 初始化（规格 nodegraph-variable-table §2.3） ----------

    private static GraphData graphWithVars(List<io.github.tt432.eyelib.nodegraph.NodeInstance> nodes,
                                           List<io.github.tt432.eyelib.nodegraph.Wire> wires,
                                           List<io.github.tt432.eyelib.nodegraph.VariableDecl> variables) {
        return new GraphData(nodes, wires, variables, List.of(), List.of(), Optional.empty());
    }

    @Test
    void variableDefaultsExportToInitialize() {
        GraphData main = graphWithVars(
                List.of(node("root", "entity.root", opts("identifier", "test:inits"))),
                List.of(),
                List.of(new io.github.tt432.eyelib.nodegraph.VariableDecl("hp", PortType.FLOAT,
                                Optional.empty(), Optional.of(new com.google.gson.JsonPrimitive(3)),
                                io.github.tt432.eyelib.nodegraph.VariableDecl.Scope.VARIABLE),
                        // TEMP 作用域默认值不导出；无默认值不导出
                        new io.github.tt432.eyelib.nodegraph.VariableDecl("scratch", PortType.FLOAT,
                                Optional.empty(), Optional.of(new com.google.gson.JsonPrimitive(9)),
                                io.github.tt432.eyelib.nodegraph.VariableDecl.Scope.TEMP),
                        io.github.tt432.eyelib.nodegraph.VariableDecl.of("nodef", PortType.FLOAT)));

        AssemblyResult r = ClientEntityAssembler.assemble(lib(GraphKind.CLIENT_ENTITY, main));

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject scripts = r.json().getAsJsonObject("minecraft:client_entity")
                .getAsJsonObject("description").getAsJsonObject("scripts");
        assertEquals("variable.hp = 3", scripts.get("initialize").getAsString());
    }

    @Test
    void variableDefaultsPrependUserInitialize() {
        GraphData main = graphWithVars(
                List.of(node("root", "entity.root", opts("identifier", "test:inits2")),
                        // v13：initialize 链挂 event.initialize 事件源节点
                        node("ev1", "event.initialize"),
                        node("s1", "exec.set_var"),
                        node("s1t", "variable", opts("name", "foo"))),
                List.of(wire("s1", "target", "s1t", "in"),
                        // v13：event.initialize.exec_out → 链首 exec_in；链尾 exec_out 悬空
                        wire("ev1", "exec_out", "s1", "exec_in")),
                List.of(new io.github.tt432.eyelib.nodegraph.VariableDecl("hp", PortType.FLOAT,
                        Optional.empty(), Optional.of(new com.google.gson.JsonPrimitive(3)),
                        io.github.tt432.eyelib.nodegraph.VariableDecl.Scope.VARIABLE)));

        AssemblyResult r = ClientEntityAssembler.assemble(lib(GraphKind.CLIENT_ENTITY, main));

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject scripts = r.json().getAsJsonObject("minecraft:client_entity")
                .getAsJsonObject("description").getAsJsonObject("scripts");
        // 初始化在前（用户语句可覆盖初始化值）
        assertEquals("variable.hp = 3; variable.foo = 0", scripts.get("initialize").getAsString());
    }

    // ---------- v13：无 event 节点 / event 节点 exec_out 悬空 → scripts 槽不输出 ----------

    @Test
    void unanchoredChainEmitsNoScript() {
        // 无 event 节点：exec 链不接任何执行时机锚点 → initialize 不输出
        // （ORPHAN_CHAIN 警告由 GraphValidator 产生，覆盖见 GraphValidatorTest.orphanChain）
        GraphLibrary lib = lib(GraphKind.CLIENT_ENTITY, graph(
                List.of(
                        node("root", "entity.root", opts("identifier", "test:noevent")),
                        node("s1", "exec.set_var"),
                        node("s1t", "variable", opts("name", "foo"))),
                List.of(wire("s1", "target", "s1t", "in"))));

        AssemblyResult r = ClientEntityAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject desc = r.json().getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
        assertFalse(desc.has("scripts"));
    }

    @Test
    void eventNodeUnwiredEmitsNoScript() {
        // event.initialize 在场但 exec_out 悬空（链未接到事件源）→ initialize 不输出
        GraphLibrary lib = lib(GraphKind.CLIENT_ENTITY, graph(
                List.of(
                        node("root", "entity.root", opts("identifier", "test:evunwired")),
                        node("ev1", "event.initialize"),
                        node("s1", "exec.set_var"),
                        node("s1t", "variable", opts("name", "foo"))),
                List.of(wire("s1", "target", "s1t", "in"))));

        AssemblyResult r = ClientEntityAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject desc = r.json().getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
        assertFalse(desc.has("scripts"));
    }
}
