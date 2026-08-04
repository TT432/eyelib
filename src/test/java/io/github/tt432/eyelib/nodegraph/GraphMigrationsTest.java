package io.github.tt432.eyelib.nodegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * {@link GraphMigrations}：format_version 1 → 2 变量节点化迁移契约（规格 §3.4）。
 */
class GraphMigrationsTest {

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
}
