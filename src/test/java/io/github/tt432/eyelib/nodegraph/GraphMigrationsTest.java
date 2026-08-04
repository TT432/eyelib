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
 * + 2 → 3 声明连线化迁移契约（规格 nodegraph-declaration-wiring §2.4）。
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
                        node("e1", "animate.entry")),
                List.of(wire("a1", "ref", "e1", "ref"), wire("e1", "entry", "root", "animate")))));

        GraphLibrary migrated = GraphMigrations.migrate(old);

        assertEquals(GraphLibrary.CURRENT_FORMAT_VERSION, migrated.formatVersion());
        GraphData main = migrated.mainGraph();
        // 无连线的 ref.geometry 补线；仅接 animate.entry 的 ref.animation 也补线（原连线保留）
        assertTrue(main.wires().contains(wire("g1", "ref", "root", "geometries")));
        assertTrue(main.wires().contains(wire("a1", "ref", "root", "animations")));
        assertTrue(main.wires().contains(wire("a1", "ref", "e1", "ref")));
    }

    @Test
    void wiredDeclarationRefNotDoubleWired() {
        GraphLibrary old = library(2, Map.of("root", graph(
                List.of(rootNode(), node("g1", "ref.geometry", opts("identifier", "geometry.a"))),
                List.of(wire("g1", "ref", "root", "geometries")))));

        GraphData main = GraphMigrations.migrate(old).mainGraph();

        assertEquals(1, main.wires().stream()
                .filter(w -> w.from().node().equals("g1") && w.to().port().equals("geometries")).count());
    }

    @Test
    void bareRefRcGainsConditionEntry() {
        GraphLibrary old = library(2, Map.of("root", graph(
                List.of(rootNode(), node("rc1", "ref.rc", opts("identifier", "controller.render.a"))),
                List.of())));

        GraphLibrary migrated = GraphMigrations.migrate(old);

        GraphData main = migrated.mainGraph();
        NodeInstance entry = main.nodes().stream()
                .filter(n -> n.type().equals("rc.condition_entry")).findFirst().orElseThrow();
        // 置于 ref 右侧；condition 不设内联值（端口默认 1）
        assertEquals(240f, entry.x());
        assertTrue(entry.constants().isEmpty());
        assertTrue(main.wires().contains(wire("rc1", "ref", entry.uid(), "rc")));
        assertTrue(main.wires().contains(wire(entry.uid(), "entry", "root", "render_controllers")));
        // 组装产物：condition 恒 1 → 纯字符串
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
                "root", graph(List.of(rootNode()), List.of()),
                "sub", sub));

        GraphLibrary migrated = GraphMigrations.migrate(old);

        GraphData main = migrated.mainGraph();
        // 完全无连线的 t9 移入主图（uid/选项保留）并补线
        NodeInstance moved = main.findNode("t9").orElseThrow();
        assertEquals("textures/a", moved.options().get("path").getAsString());
        assertTrue(main.wires().contains(wire("t9", "ref", "root", "textures")));
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
                        node("rc1", "ref.rc", opts("identifier", "controller.render.a")),
                        // 迁移生成的 rc.condition_entry（uid 依 freshUid 序列）
                        node("mig0", "rc.condition_entry", Map.of())),
                List.of(wire("a1", "ref", "e1", "ref"), wire("e1", "entry", "root", "animate"),
                        wire("g1", "ref", "root", "geometries"),
                        wire("a1", "ref", "root", "animations"),
                        wire("rc1", "ref", "mig0", "rc"),
                        wire("mig0", "entry", "root", "render_controllers")))));

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
}
