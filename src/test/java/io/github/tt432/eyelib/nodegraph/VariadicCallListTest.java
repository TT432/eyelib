package io.github.tt432.eyelib.nodegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.codegen.CodegenResult;
import io.github.tt432.eyelib.nodegraph.codegen.MolangGenerator;
import io.github.tt432.eyelib.nodegraph.decompile.MolangDecompiler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * v11 变长 call 参数列表化（规格 nodegraph-variadic-call-list）契约测试：
 * args 字面值列表选项取代 arg_count + argN 变长端口；定长签名端口不变。
 */
class VariadicCallListTest {

    private static NodeInstance node(String uid, String type) {
        return NodeInstance.of(uid, type, 0, 0);
    }

    private static NodeInstance node(String uid, String type, Map<String, JsonElement> options) {
        return new NodeInstance(uid, type, 0, 0, options, Map.of());
    }

    private static NodeInstance node(String uid, String type, Map<String, JsonElement> options,
                                     Map<String, JsonElement> constants) {
        return new NodeInstance(uid, type, 0, 0, options, constants);
    }

    private static Map<String, JsonElement> opts(Object... kv) {
        Map<String, JsonElement> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            JsonElement e = v instanceof String s ? new JsonPrimitive(s)
                    : v instanceof Number n ? new JsonPrimitive(n)
                    : v instanceof Boolean b ? new JsonPrimitive(b)
                    : (JsonElement) v;
            map.put((String) kv[i], e);
        }
        return map;
    }

    private static JsonArray args(Object... values) {
        JsonArray array = new JsonArray();
        for (Object v : values) {
            if (v instanceof String s) {
                array.add(s);
            } else if (v instanceof Number n) {
                array.add(n);
            } else if (v instanceof Boolean b) {
                array.add(b);
            }
        }
        return array;
    }

    private static Wire wire(String fromNode, String fromPort, String toNode, String toPort) {
        return new Wire(new PortRef(fromNode, fromPort), new PortRef(toNode, toPort));
    }

    private static GraphData graph(List<NodeInstance> nodes, List<Wire> wires) {
        return new GraphData(nodes, wires, List.of(), List.of(), List.of(), Optional.empty());
    }

    private static GraphLibrary library(int version, GraphData main) {
        return new GraphLibrary(version, GraphKind.CLIENT_ENTITY, "root", Map.of("root", main));
    }

    private static String expr(GraphLibrary lib) {
        CodegenResult r = new MolangGenerator(lib).emitExpressionFor("root", "root", "scale");
        assertFalse(r.hasErrors(), () -> "unexpected errors: " + r.diagnostics());
        return r.code();
    }

    // ---------- 发射 ----------

    @Test
    void variadicArgsEmitFromList() {
        GraphData main = graph(
                List.of(node("root", "entity.root"),
                        node("q", "query.call", opts("function", "query.is_name_any",
                                "args", args("a", "b")))),
                List.of(wire("q", "out", "root", "scale")));
        assertEquals("query.is_name_any('a', 'b')", expr(library(11, main)));
    }

    @Test
    void unknownFunctionArgsEmitFromList() {
        GraphData main = graph(
                List.of(node("root", "entity.root"),
                        node("q", "query.call", opts("function", "query.foo",
                                "args", args(2, "x", true)))),
                List.of(wire("q", "out", "root", "scale")));
        assertEquals("query.foo(2, 'x', 1)", expr(library(11, main)));
    }

    @Test
    void emptyListEntriesSkipped() {
        GraphData main = graph(
                List.of(node("root", "entity.root"),
                        node("q", "query.call", opts("function", "query.is_name_any",
                                "args", args("a", "", "b")))),
                List.of(wire("q", "out", "root", "scale")));
        assertEquals("query.is_name_any('a', 'b')", expr(library(11, main)));
    }

    @Test
    void fixedArityPortsUnchanged() {
        // math.pow 定长双参：argN 端口照常（行内常量 + 连线）
        GraphData main = graph(
                List.of(node("root", "entity.root"),
                        node("m", "math.call", opts("function", "math.pow"),
                                Map.of("arg2", new JsonPrimitive(2))),
                        node("q", "query.call", opts("function", "query.anim_time"))),
                List.of(wire("m", "out", "root", "scale"),
                        wire("q", "out", "m", "arg1")));
        assertEquals("math.pow(query.anim_time, 2)", expr(library(11, main)));
    }

    @Test
    void zeroArgCallKeepsPropertyForm() {
        GraphData main = graph(
                List.of(node("root", "entity.root"),
                        node("q", "query.call", opts("function", "query.anim_time"))),
                List.of(wire("q", "out", "root", "scale")));
        assertEquals("query.anim_time", expr(library(11, main)));
    }

    @Test
    void variadicSignatureKeepsOnlyFixedPrefixPorts() {
        // query.is_item_name_any：fixed=[hand]，varArg=items → 仅 arg1 端口，变长尾参进列表
        NodeInstance call = node("q", "query.call", opts("function", "query.is_item_name_any"));
        var type = NodeTypes.get("query.call").orElseThrow();
        List<PortDef> inputs = type.inputsOf(call, name -> Optional.empty());
        assertEquals(1, inputs.size());
        assertEquals("arg1", inputs.get(0).id());
        assertEquals("hand: str", inputs.get(0).label().orElseThrow());
    }

    @Test
    void unknownFunctionHasNoArgPorts() {
        NodeInstance call = node("q", "query.call", opts("function", "query.foo"));
        var type = NodeTypes.get("query.call").orElseThrow();
        assertTrue(type.inputsOf(call, name -> Optional.empty()).isEmpty());
    }

    // ---------- 反编译 ----------

    @Test
    void decompileVariadicCallToList() {
        MolangDecompiler.ExprFragment f = MolangDecompiler.decompileExpression("q.is_name_any('a', 'b')");
        NodeInstance call = f.nodes().stream().filter(n -> "query.call".equals(n.type()))
                .findFirst().orElseThrow();
        assertEquals("query.is_name_any", call.options().get("function").getAsString());
        assertEquals(args("a", "b"), call.options().get("args"));
        assertFalse(call.options().containsKey("arg_count"), "arg_count 已消除");
        assertTrue(f.wires().stream().noneMatch(w -> w.to().node().equals(call.uid())),
                "变长实参不产生连线");
    }

    @Test
    void decompileFixedArityCallWiresPorts() {
        MolangDecompiler.ExprFragment f = MolangDecompiler.decompileExpression("math.sin(query.anim_time)");
        NodeInstance call = f.nodes().stream().filter(n -> "math.call".equals(n.type()))
                .findFirst().orElseThrow();
        assertFalse(call.options().containsKey("args"), "定长签名不走列表");
        assertTrue(f.wires().stream().anyMatch(w -> w.to().node().equals(call.uid())
                && "arg1".equals(w.to().port())), "定长实参连线 arg1");
    }

    @Test
    void decompileNonLiteralVariadicArgSkippedWithDiagnostic() {
        MolangDecompiler.ExprFragment f = MolangDecompiler.decompileExpression(
                "q.is_name_any('a', q.is_baby)");
        NodeInstance call = f.nodes().stream().filter(n -> "query.call".equals(n.type()))
                .findFirst().orElseThrow();
        assertEquals(args("a"), call.options().get("args"), "非字面量实参跳过");
        assertFalse(f.diagnostics().isEmpty(), "非字面量实参应有诊断");
    }

    // ---------- 迁移 v10 → v11 ----------

    @Test
    void migrateHarvestsVariadicConstantsIntoList() {
        Map<String, JsonElement> constants = new LinkedHashMap<>();
        constants.put("arg1", new JsonPrimitive("a"));
        constants.put("arg2", new JsonPrimitive("b"));
        GraphData main = graph(
                List.of(node("root", "entity.root"),
                        new NodeInstance("q", "query.call", 0, 0,
                                opts("function", "query.is_name_any", "arg_count", 2), constants)),
                List.of(wire("q", "out", "root", "scale")));
        GraphLibrary migrated = GraphMigrations.migrate(library(10, main));
        assertEquals(11, migrated.formatVersion());
        NodeInstance q = migrated.mainGraph().findNode("q").orElseThrow();
        assertFalse(q.options().containsKey("arg_count"));
        assertEquals(args("a", "b"), q.options().get("args"));
        assertFalse(q.constants().containsKey("arg1"));
        assertFalse(q.constants().containsKey("arg2"));
    }

    @Test
    void migrateFixedArityOnlyDropsArgCount() {
        GraphData main = graph(
                List.of(node("root", "entity.root"),
                        node("m", "math.call", opts("function", "math.sin", "arg_count", 1),
                                Map.of("arg1", new JsonPrimitive(2)))),
                List.of(wire("m", "out", "root", "scale")));
        GraphLibrary migrated = GraphMigrations.migrate(library(10, main));
        NodeInstance m = migrated.mainGraph().findNode("m").orElseThrow();
        assertFalse(m.options().containsKey("arg_count"));
        assertFalse(m.options().containsKey("args"), "定长函数不产生列表");
        assertEquals(new JsonPrimitive(2), m.constants().get("arg1"), "固定前缀常量保留");
    }

    @Test
    void migrateHarvestsWiredConstAndRemovesOrphan() {
        GraphData main = graph(
                List.of(node("root", "entity.root"),
                        node("q", "query.call", opts("function", "query.is_name_any", "arg_count", 1)),
                        node("c", "const.string", opts("value", "a"))),
                new ArrayList<>(List.of(wire("q", "out", "root", "scale"),
                        wire("c", "out", "q", "arg1"))));
        GraphLibrary migrated = GraphMigrations.migrate(library(10, main));
        GraphData g = migrated.mainGraph();
        NodeInstance q = g.findNode("q").orElseThrow();
        assertEquals(args("a"), q.options().get("args"), "连线 const 收进列表");
        assertTrue(g.findNode("c").isEmpty(), "孤儿 const 节点删除");
        assertTrue(g.wires().stream().noneMatch(w -> "c".equals(w.from().node())), "const 连线删除");
    }
}
