package io.github.tt432.eyelib.nodegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.assembly.AssemblyResult;
import io.github.tt432.eyelib.nodegraph.assembly.ClientEntityAssembler;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * {@link GraphMigrations}：format_version 1 → 2 变量节点化迁移契约（规格 §3.4）
 * + 2 → 3 声明连线化迁移契约（规格 nodegraph-declaration-wiring §2.4）
 * + 3 → 4 RenderController 内联迁移契约（规格 nodegraph-inline-render-controller §5）。
 * v2 起点用例走完整迁移链，断言的是 v4 终态。
 */
class GraphMigrationsTest {

    private static NodeInstance node(String uid, String type) {
        return NodeInstance.of(uid, type, 0, 0);
    }

    private static NodeInstance node(String uid, String type, Map<String, JsonElement> options) {
        return new NodeInstance(uid, type, 0, 0, options, Map.of());
    }

    private static Map<String, JsonElement> opts(Object... kv) {
        Map<String, JsonElement> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            map.put((String) kv[i], v instanceof JsonElement e ? e : new JsonPrimitive(String.valueOf(v)));
        }
        return map;
    }

    private static GraphLibrary library(GraphData main) {
        return new GraphLibrary(1, GraphKind.CLIENT_ENTITY, "root", Map.of("root", main));
    }

    private static GraphLibrary library(int version, Map<String, GraphData> graphs) {
        return new GraphLibrary(version, GraphKind.CLIENT_ENTITY, "root", graphs);
    }

    private static GraphData graph(List<NodeInstance> nodes, List<Wire> wires) {
        return new GraphData(nodes, wires, List.of(), List.of(), List.of(), Optional.empty());
    }

    private static Wire wire(String fromNode, String fromPort, String toNode, String toPort) {
        return new Wire(new PortRef(fromNode, fromPort), new PortRef(toNode, toPort));
    }

    private static NodeInstance rootNode() {
        return node("root", "entity.root", opts("identifier", "test:mig"));
    }

    @Test
    void varGetBecomesVariableNode() {
        GraphLibrary migrated = GraphMigrations.migrate(library(new GraphData(
                List.of(node("v", "var.get", opts("name", "variable.foo"))),
                List.of(), List.of(), List.of(), List.of(), Optional.empty())));
        NodeInstance node = migrated.mainGraph().nodes().get(0);
        assertEquals("variable", node.type());
        assertEquals("foo", node.options().get("name").getAsString());
        assertEquals(GraphLibrary.CURRENT_FORMAT_VERSION, migrated.formatVersion());
    }

    @Test
    void setVarVariableRootGainsTargetVariableNode() {
        GraphLibrary migrated = GraphMigrations.migrate(library(new GraphData(
                List.of(node("s", "exec.set_var", opts("root", "variable", "name", "variable.foo"))),
                List.of(), List.of(), List.of(), List.of(), Optional.empty())));
        GraphData graph = migrated.mainGraph();
        NodeInstance setVar = graph.findNode("s").orElseThrow();
        assertEquals("exec.set_var", setVar.type());
        assertTrue(setVar.options().isEmpty());
        // 自动 variable 节点 + target 连线
        Wire targetWire = graph.wires().stream()
                .filter(w -> w.to().node().equals("s") && w.to().port().equals("target"))
                .findFirst().orElseThrow();
        NodeInstance varNode = graph.findNode(targetWire.from().node()).orElseThrow();
        assertEquals("variable", varNode.type());
        assertEquals("foo", varNode.options().get("name").getAsString());
        assertEquals("out", targetWire.from().port());
    }

    @Test
    void setVarTempRootBecomesSetTemp() {
        GraphLibrary migrated = GraphMigrations.migrate(library(new GraphData(
                List.of(node("s", "exec.set_var", opts("root", "temp", "name", "temp.t"))),
                List.of(), List.of(), List.of(), List.of(), Optional.empty())));
        NodeInstance node = migrated.mainGraph().nodes().get(0);
        assertEquals("exec.set_temp", node.type());
        assertEquals("temp.t", node.options().get("name").getAsString());
    }

    @Test
    void newFormatPassesThrough() {
        GraphData data = new GraphData(
                List.of(node("v", "variable", opts("name", "foo")),
                        node("s", "exec.set_var", Map.of())),
                List.of(new Wire(new PortRef("v", "out"), new PortRef("s", "target"))),
                List.of(VariableDecl.of("foo", PortType.FLOAT)),
                List.of(), List.of(), Optional.empty());
        GraphLibrary migrated = GraphMigrations.migrate(library(data));
        assertEquals(data.nodes(), migrated.mainGraph().nodes());
        assertEquals(data.wires(), migrated.mainGraph().wires());
        assertEquals(data.variables(), migrated.mainGraph().variables());
    }

    @Test
    void migrationPreservesVariablesAndValueWires() {
        GraphLibrary migrated = GraphMigrations.migrate(library(new GraphData(
                List.of(node("s", "exec.set_var", opts("root", "variable", "name", "a")),
                        node("c", "const.number", opts("value", 1))),
                List.of(new Wire(new PortRef("c", "out"), new PortRef("s", "value"))),
                List.of(VariableDecl.of("a", PortType.FLOAT)),
                List.of(), List.of(), Optional.empty())));
        GraphData graph = migrated.mainGraph();
        assertEquals(1, graph.variables().size());
        // 既有 value 连线保留
        assertTrue(graph.wires().stream()
                .anyMatch(w -> w.from().node().equals("c") && w.to().node().equals("s")
                        && w.to().port().equals("value")));
    }

    // ---------- v2 → v3：声明连线化（规格 §2.4） ----------

    @Test
    void declarationRefsGetWiredToRootPorts() {
        GraphLibrary old = library(2, Map.of("root", graph(
                List.of(rootNode(),
                        node("g1", "ref.geometry", opts("identifier", "geometry.a")),
                        node("a1", "ref.animation", opts("short_name", "walk", "identifier", "animation.a")),
                        node("e1", "animate.entry"),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.a"))),
                List.of(wire("a1", "ref", "e1", "ref"), wire("e1", "entry", "root", "animate")))));

        GraphLibrary migrated = GraphMigrations.migrate(old);

        assertEquals(GraphLibrary.CURRENT_FORMAT_VERSION, migrated.formatVersion());
        GraphData main = migrated.mainGraph();
        // v2→v3 补线 + v3→v4 重定向：ref.geometry 最终接主图第一个 ref.rc（uid 序）的声明端口；
        // ref.animation 保留实体级 animations 端口（原连线保留）；ref.rc 直连 render_controllers
        assertTrue(main.wires().contains(wire("g1", "ref", "rc1", "decl_geometries")));
        assertTrue(main.wires().contains(wire("a1", "ref", "root", "animations")));
        assertTrue(main.wires().contains(wire("a1", "ref", "e1", "ref")));
        assertTrue(main.wires().contains(wire("rc1", "ref", "root", "render_controllers")));
        assertTrue(main.nodes().stream().noneMatch(n -> n.type().equals("rc.condition_entry")));
    }

    @Test
    void wiredDeclarationRefNotDoubleWired() {
        GraphLibrary old = library(2, Map.of("root", graph(
                List.of(rootNode(), node("g1", "ref.geometry", opts("identifier", "geometry.a")),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.a"))),
                List.of(wire("g1", "ref", "root", "geometries")))));

        GraphData main = GraphMigrations.migrate(old).mainGraph();

        // v4：既有声明线重定向第一个 ref.rc，不重复补线
        assertEquals(1, main.wires().stream()
                .filter(w -> w.from().node().equals("g1") && w.to().port().equals("decl_geometries")).count());
        assertTrue(main.wires().contains(wire("g1", "ref", "rc1", "decl_geometries")));
    }

    @Test
    void bareRefRcMountedDirectly() {
        // v2 裸 ref.rc：v2→v3 生成 rc.condition_entry，v3→v4 随即拆解为直连
        GraphLibrary old = library(2, Map.of("root", graph(
                List.of(rootNode(), node("rc1", "ref.rc", opts("identifier", "controller.render.a"))),
                List.of())));

        GraphLibrary migrated = GraphMigrations.migrate(old);

        GraphData main = migrated.mainGraph();
        assertTrue(main.nodes().stream().noneMatch(n -> n.type().equals("rc.condition_entry")));
        assertTrue(main.wires().contains(wire("rc1", "ref", "root", "render_controllers")));
        // condition 未设置 → 端口默认 1 → 组装产物为纯字符串
        AssemblyResult assembled = ClientEntityAssembler.assemble(migrated);
        JsonArray rcs = assembled.json().getAsJsonObject("minecraft:client_entity")
                .getAsJsonObject("description").getAsJsonArray("render_controllers");
        assertEquals("controller.render.a", rcs.get(0).getAsString());
    }

    @Test
    void subgraphUnwiredRefMovedToMainAndWired() {
        GraphInterface iface = new GraphInterface(List.of(), GraphInterface.Param.of("result", PortType.FLOAT));
        GraphData sub = new GraphData(
                List.of(node("out", "subgraph.output"), node("c", "op.binary", opts("op", "+")),
                        node("t9", "ref.texture", opts("path", "textures/a")),
                        node("t8", "ref.texture", opts("path", "textures/b"))),
                List.of(wire("c", "out", "out", "result"), wire("t8", "ref", "c", "a")),
                List.of(), List.of(), List.of(), Optional.of(iface));
        GraphLibrary old = library(2, Map.of(
                "root", graph(List.of(rootNode(),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.a"))), List.of()),
                "sub", sub));

        GraphLibrary migrated = GraphMigrations.migrate(old);

        GraphData main = migrated.mainGraph();
        // 完全无连线的 t9 移入主图（uid/选项保留）并补线（v4：重定向第一个 ref.rc 的声明端口）
        NodeInstance moved = main.findNode("t9").orElseThrow();
        assertEquals("textures/a", moved.options().get("path").getAsString());
        assertTrue(main.wires().contains(wire("t9", "ref", "rc1", "decl_textures")));
        GraphData migratedSub = migrated.graphs().get("sub");
        assertTrue(migratedSub.findNode("t9").isEmpty());
        // 有连线的 t8 不动
        assertTrue(migratedSub.findNode("t8").isPresent());
        assertTrue(main.findNode("t8").isEmpty());
    }

    @Test
    void migratedScanGraphAssemblesSameAsHandWired() {
        // 旧扫描图（v2，无任何声明连线）迁移后构建产物 = 手工连线图构建产物
        GraphLibrary old = library(2, Map.of("root", graph(
                List.of(rootNode(),
                        node("g1", "ref.geometry", opts("short_name", "default", "identifier", "geometry.a")),
                        node("a1", "ref.animation", opts("short_name", "walk", "identifier", "animation.a")),
                        node("e1", "animate.entry"),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.a"))),
                List.of(wire("a1", "ref", "e1", "ref"), wire("e1", "entry", "root", "animate")))));
        GraphLibrary handWired = new GraphLibrary(GraphLibrary.CURRENT_FORMAT_VERSION,
                GraphKind.CLIENT_ENTITY, "root", Map.of("root", graph(
                List.of(rootNode(),
                        node("g1", "ref.geometry", opts("short_name", "default", "identifier", "geometry.a")),
                        node("a1", "ref.animation", opts("short_name", "walk", "identifier", "animation.a")),
                        node("e1", "animate.entry"),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.a"))),
                // v4 手工终态：geo 声明线接第一个 ref.rc；ref.rc 直连 render_controllers
                List.of(wire("a1", "ref", "e1", "ref"), wire("e1", "entry", "root", "animate"),
                        wire("g1", "ref", "rc1", "decl_geometries"),
                        wire("a1", "ref", "root", "animations"),
                        wire("rc1", "ref", "root", "render_controllers")))));

        AssemblyResult fromMigrated = ClientEntityAssembler.assemble(GraphMigrations.migrate(old));
        AssemblyResult fromWired = ClientEntityAssembler.assemble(handWired);

        assertFalse(fromMigrated.hasErrors(), () -> fromMigrated.diagnostics().toString());
        assertEquals(fromWired.json(), fromMigrated.json());
    }

    @Test
    void nonClientEntityLibraryUntouched() {
        GraphData main = graph(
                List.of(node("root", "rc.root", opts("identifier", "controller.render.a")),
                        node("m1", "ref.material", opts("short_name", "default"))),
                List.of());
        GraphLibrary old = new GraphLibrary(2, GraphKind.RENDER_CONTROLLER, "root", Map.of("root", main));

        GraphLibrary migrated = GraphMigrations.migrate(old);

        assertEquals(GraphLibrary.CURRENT_FORMAT_VERSION, migrated.formatVersion());
        assertEquals(main, migrated.mainGraph());
    }

    @Test
    void migrationIsIdempotent() {
        GraphLibrary old = library(2, Map.of("root", graph(
                List.of(rootNode(),
                        node("g1", "ref.geometry", opts("identifier", "geometry.a")),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.a"))),
                List.of())));
        GraphLibrary once = GraphMigrations.migrate(old);
        assertEquals(once, GraphMigrations.migrate(once));
    }

    // ---------- v3 → v4：RenderController 内联（规格 inline-render-controller §5） ----------

    private static GraphLibrary v3Library(GraphData main) {
        return new GraphLibrary(3, GraphKind.CLIENT_ENTITY, "root", Map.of("root", main));
    }

    private static NodeInstance conditionEntry(String uid, String inlineCondition) {
        return new NodeInstance(uid, "rc.condition_entry", 0, 0, Map.of(),
                opts("condition", inlineCondition));
    }

    @Test
    void conditionEntryInlineConstantMovesToRefRc() {
        // entry 的内联 condition 常量搬到 ref.rc.condition；entry 及其线删除；ref 直连 render_controllers
        GraphLibrary migrated = GraphMigrations.migrate(v3Library(graph(
                List.of(rootNode(),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.a")),
                        conditionEntry("e1", "query.is_baby")),
                List.of(wire("rc1", "ref", "e1", "rc"),
                        wire("e1", "entry", "root", "render_controllers")))));

        assertEquals(GraphLibrary.CURRENT_FORMAT_VERSION, migrated.formatVersion());
        GraphData main = migrated.mainGraph();
        assertTrue(main.findNode("e1").isEmpty());
        assertTrue(main.wires().stream()
                .noneMatch(w -> w.from().node().equals("e1") || w.to().node().equals("e1")));
        assertTrue(main.wires().contains(wire("rc1", "ref", "root", "render_controllers")));
        assertEquals("query.is_baby",
                main.findNode("rc1").orElseThrow().constants().get("condition").getAsString());
    }

    @Test
    void conditionEntryConditionWireMovedToRefRc() {
        // condition 连线（非内联）迁到 ref.rc.condition，线源保留
        GraphLibrary migrated = GraphMigrations.migrate(v3Library(graph(
                List.of(rootNode(),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.a")),
                        node("e1", "rc.condition_entry"),
                        node("q1", "query.call", opts("function", "query.is_baby"))),
                List.of(wire("rc1", "ref", "e1", "rc"),
                        wire("q1", "out", "e1", "condition"),
                        wire("e1", "entry", "root", "render_controllers")))));

        GraphData main = migrated.mainGraph();
        assertTrue(main.findNode("e1").isEmpty());
        assertTrue(main.wires().contains(wire("q1", "out", "rc1", "condition")));
        assertTrue(main.wires().contains(wire("rc1", "ref", "root", "render_controllers")));
    }

    @Test
    void doubleEntrySameRefFirstWins() {
        // 同一 ref.rc 多条 entry：先者（节点序）的 condition 胜出；render_controllers 只挂一次；其余 entry 删除
        GraphLibrary migrated = GraphMigrations.migrate(v3Library(graph(
                List.of(rootNode(),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.a")),
                        conditionEntry("e1", "query.a"),
                        conditionEntry("e2", "query.b")),
                List.of(wire("rc1", "ref", "e1", "rc"), wire("e1", "entry", "root", "render_controllers"),
                        wire("rc1", "ref", "e2", "rc"), wire("e2", "entry", "root", "render_controllers")))));

        GraphData main = migrated.mainGraph();
        assertTrue(main.findNode("e1").isEmpty());
        assertTrue(main.findNode("e2").isEmpty());
        assertEquals("query.a",
                main.findNode("rc1").orElseThrow().constants().get("condition").getAsString());
        assertEquals(1, main.wires().stream().filter(w -> w.from().node().equals("rc1")
                && w.to().node().equals("root") && w.to().port().equals("render_controllers")).count());
    }

    @Test
    void declarationWiresRedirectedToFirstRefRc() {
        // entity.root 三声明端口的线 → 重定向 uid 序最小的 ref.rc
        GraphLibrary migrated = GraphMigrations.migrate(v3Library(graph(
                List.of(rootNode(),
                        node("g1", "ref.geometry", opts("identifier", "geometry.a")),
                        node("t1", "ref.texture", opts("path", "textures/a")),
                        node("rc2", "ref.rc", opts("identifier", "controller.render.b")),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.a"))),
                List.of(wire("g1", "ref", "root", "geometries"),
                        wire("t1", "ref", "root", "textures")))));

        GraphData main = migrated.mainGraph();
        assertTrue(main.wires().contains(wire("g1", "ref", "rc1", "decl_geometries")));
        assertTrue(main.wires().contains(wire("t1", "ref", "rc1", "decl_textures")));
        assertTrue(main.wires().stream().noneMatch(w -> w.to().node().equals("root")
                && java.util.Set.of("geometries", "textures", "materials").contains(w.to().port())));
    }

    @Test
    void declarationWiresDroppedWithoutRefRc() {
        // 无 ref.rc → 声明线断线（节点保留，验证器 REF_NOT_CONNECTED 提示）
        GraphLibrary migrated = GraphMigrations.migrate(v3Library(graph(
                List.of(rootNode(),
                        node("g1", "ref.geometry", opts("identifier", "geometry.a"))),
                List.of(wire("g1", "ref", "root", "geometries")))));

        GraphData main = migrated.mainGraph();
        assertTrue(main.wires().isEmpty());
        assertTrue(main.findNode("g1").isPresent());
    }

    @Test
    void v4LibraryOnlyBumpsVersion() {
        // v4 图无颜色内容：v5 迁移无介入点，图内容原样，版本升到 CURRENT
        GraphData main = graph(
                List.of(rootNode(),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.a"))),
                List.of(wire("rc1", "ref", "root", "render_controllers")));
        GraphLibrary v4 = new GraphLibrary(4, GraphKind.CLIENT_ENTITY, "root", Map.of("root", main));
        GraphLibrary migrated = GraphMigrations.migrate(v4);
        assertEquals(GraphLibrary.CURRENT_FORMAT_VERSION, migrated.formatVersion());
        assertEquals(main, migrated.mainGraph());
    }

    // ---------- v4 → v5：rc.root 颜色通道端口 → COLOR 端口 ----------

    @Test
    void byteExactChannelConstantsBecomeConstColor() {
        // color_r=1, color_g=0, color_b=0（a 无 = 默认 1），全 8bit 精确 → const.color #FFFF0000
        NodeInstance rc = new NodeInstance("root", "rc.root", 0, 0,
                opts("identifier", "controller.render.a"),
                Map.of("color_r", new JsonPrimitive(1),
                        "color_g", new JsonPrimitive(0),
                        "color_b", new JsonPrimitive(0)));
        GraphLibrary old = new GraphLibrary(4, GraphKind.RENDER_CONTROLLER, "root",
                Map.of("root", graph(List.of(rc), List.of())));

        GraphData main = GraphMigrations.migrate(old).mainGraph();

        NodeInstance migratedRc = main.findNode("root").orElseThrow();
        assertTrue(migratedRc.constants().isEmpty());
        Optional<NodeInstance> cc = main.nodes().stream()
                .filter(n -> n.type().equals("const.color")).findFirst();
        assertTrue(cc.isPresent(), () -> main.nodes().toString());
        assertEquals("#FFFF0000", cc.get().options().get("value").getAsString());
        assertTrue(main.wires().contains(wire(cc.get().uid(), "out", "root", "color")));
    }

    @Test
    void wiredChannelBecomesCompose() {
        // overlay_r 有表达式线、overlay_a=0.5 内联常数（非 8bit 精确）→ color.compose：
        // 线移 compose.r、常数移 compose.a、out 接 root.overlay_color、rc 常数清除
        NodeInstance rc = new NodeInstance("root", "rc.root", 0, 0,
                opts("identifier", "controller.render.a"),
                Map.of("overlay_a", new JsonPrimitive(0.5)));
        GraphLibrary old = new GraphLibrary(4, GraphKind.RENDER_CONTROLLER, "root",
                Map.of("root", graph(
                        List.of(rc, node("e", "const.number", opts("value", 0.25))),
                        List.of(wire("e", "out", "root", "overlay_r")))));

        GraphData main = GraphMigrations.migrate(old).mainGraph();

        NodeInstance migratedRc = main.findNode("root").orElseThrow();
        assertTrue(migratedRc.constants().isEmpty());
        Optional<NodeInstance> compose = main.nodes().stream()
                .filter(n -> n.type().equals("color.compose")).findFirst();
        assertTrue(compose.isPresent(), () -> main.nodes().toString());
        assertEquals(0.5, compose.get().constants().get("a").getAsDouble(), 1e-9);
        assertTrue(main.wires().contains(wire("e", "out", compose.get().uid(), "r")));
        assertTrue(main.wires().contains(wire(compose.get().uid(), "out", "root", "overlay_color")));
        assertTrue(main.wires().stream().noneMatch(w -> w.to().port().equals("overlay_r")));
    }

    @Test
    void rcRootWithoutColorContentUntouched() {
        GraphData main = graph(
                List.of(node("root", "rc.root", opts("identifier", "controller.render.a")),
                        node("m1", "ref.material", opts("short_name", "default"))),
                List.of());
        GraphLibrary old = new GraphLibrary(4, GraphKind.RENDER_CONTROLLER, "root", Map.of("root", main));

        GraphLibrary migrated = GraphMigrations.migrate(old);

        assertEquals(GraphLibrary.CURRENT_FORMAT_VERSION, migrated.formatVersion());
        assertEquals(main, migrated.mainGraph());
    }

    // ---------- v5 → v6：条目链化 + rc.root decl_* 移除 ----------

    @Test
    void v6ChainsMultiEntriesAndDropsRcRootDeclWires() {
        // v5 形态：两个条目直连 textures（发射序 = uid 序 le1 < le2）+ decl 声明线
        GraphData main = graph(
                List.of(node("root", "rc.root", opts("identifier", "controller.render.a")),
                        node("le1", "list.entry"), node("le2", "list.entry"),
                        node("me1", "material.entry"),
                        node("g1", "ref.geometry", opts("short_name", "default", "identifier", "geometry.a"))),
                List.of(wire("le1", "entry", "root", "textures"),
                        wire("le2", "entry", "root", "textures"),
                        wire("me1", "entry", "root", "materials"),
                        wire("g1", "ref", "root", "decl_geometries")));
        GraphLibrary old = new GraphLibrary(5, GraphKind.RENDER_CONTROLLER, "root", Map.of("root", main));

        GraphData migrated = GraphMigrations.migrate(old).mainGraph();

        // 链化：首条目保留直连，后续挂前一条目 next
        assertTrue(migrated.wires().contains(wire("le1", "entry", "root", "textures")));
        assertTrue(migrated.wires().contains(wire("le2", "entry", "le1", "next")));
        assertFalse(migrated.wires().contains(wire("le2", "entry", "root", "textures")));
        // 单条目端口不动
        assertTrue(migrated.wires().contains(wire("me1", "entry", "root", "materials")));
        // decl 线删除、ref 节点保留
        assertFalse(migrated.wires().contains(wire("g1", "ref", "root", "decl_geometries")));
        assertTrue(migrated.findNode("g1").isPresent());
    }

    @Test
    void v6KeepsRefRcDeclWires() {
        GraphData main = graph(
                List.of(rootNode(),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.a")),
                        node("g1", "ref.geometry", opts("short_name", "default", "identifier", "geometry.a"))),
                List.of(wire("rc1", "ref", "root", "render_controllers"),
                        wire("g1", "ref", "rc1", "decl_geometries")));
        GraphLibrary old = new GraphLibrary(5, GraphKind.CLIENT_ENTITY, "root", Map.of("root", main));

        GraphData migrated = GraphMigrations.migrate(old).mainGraph();

        assertTrue(migrated.wires().contains(wire("g1", "ref", "rc1", "decl_geometries")));
    }
}
