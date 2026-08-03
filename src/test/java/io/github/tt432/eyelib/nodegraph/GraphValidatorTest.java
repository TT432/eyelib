package io.github.tt432.eyelib.nodegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * {@link GraphValidator} 单测：每种检查至少一个触发用例 + 一个完整合法 client_entity 库零 ERROR。
 * 图直接用 record 构造，不走 JSON。
 */
class GraphValidatorTest {

    // ---------- 构造辅助 ----------

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

    private static Wire wire(String fromNode, String fromPort, String toNode, String toPort) {
        return new Wire(new PortRef(fromNode, fromPort), new PortRef(toNode, toPort));
    }

    private static GraphData graph(List<NodeInstance> nodes, List<Wire> wires) {
        return new GraphData(nodes, wires, List.of(), List.of(), List.of(), Optional.empty());
    }

    private static GraphData graph(List<NodeInstance> nodes, List<Wire> wires, List<VariableDecl> variables) {
        return new GraphData(nodes, wires, variables, List.of(), List.of(), Optional.empty());
    }

    private static GraphData subgraph(List<NodeInstance> nodes, List<Wire> wires, GraphInterface iface) {
        return new GraphData(nodes, wires, List.of(), List.of(), List.of(), Optional.of(iface));
    }

    private static GraphLibrary lib(GraphKind kind, GraphData main) {
        return new GraphLibrary(1, kind, "root", Map.of("root", main));
    }

    private static GraphLibrary lib(GraphKind kind, Map<String, GraphData> graphs) {
        return new GraphLibrary(1, kind, "root", graphs);
    }

    private static List<Diagnostic> validateGraph(GraphData graph) {
        return GraphValidator.validateGraph(lib(GraphKind.CLIENT_ENTITY, graph), "root", graph);
    }

    private static boolean hasCode(List<Diagnostic> diags, String code) {
        return diags.stream().anyMatch(d -> d.code().equals(code));
    }

    private static List<Diagnostic> byCode(List<Diagnostic> diags, String code) {
        return diags.stream().filter(d -> d.code().equals(code)).toList();
    }

    // ---------- hasErrors ----------

    @Test
    void hasErrorsOnlyOnErrorSeverity() {
        assertFalse(GraphValidator.hasErrors(List.of()));
        assertFalse(GraphValidator.hasErrors(List.of(Diagnostic.warning("X", "w"))));
        assertTrue(GraphValidator.hasErrors(List.of(Diagnostic.error("X", "e"))));
    }

    // ---------- 1 UNKNOWN_NODE_TYPE ----------

    @Test
    void unknownNodeType() {
        List<Diagnostic> diags = validateGraph(graph(List.of(node("n1", "no.such.type")), List.of()));
        assertTrue(hasCode(diags, GraphValidator.UNKNOWN_NODE_TYPE));
        assertEquals("n1", byCode(diags, GraphValidator.UNKNOWN_NODE_TYPE).get(0).nodeUid().orElseThrow());
    }

    // ---------- 2 DUPLICATE_UID ----------

    @Test
    void duplicateUid() {
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("n1", "const.number"), node("n1", "const.bool")), List.of()));
        assertTrue(hasCode(diags, GraphValidator.DUPLICATE_UID));
    }

    // ---------- 3 UNKNOWN_WIRE_ENDPOINT ----------

    @Test
    void unknownWireEndpointNode() {
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("c1", "const.number")),
                List.of(wire("ghost", "out", "c1", "out"))));
        assertTrue(hasCode(diags, GraphValidator.UNKNOWN_WIRE_ENDPOINT));
    }

    @Test
    void unknownWireEndpointPort() {
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("c1", "const.number"), node("c2", "const.number")),
                List.of(wire("c1", "out", "c2", "nope"))));
        assertTrue(hasCode(diags, GraphValidator.UNKNOWN_WIRE_ENDPOINT));
    }

    // ---------- 4 WIRE_DIRECTION ----------

    @Test
    void wireDirectionFromInputPort() {
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("n1", "op.binary"), node("n2", "op.binary")),
                List.of(wire("n1", "a", "n2", "a"))));
        assertTrue(hasCode(diags, GraphValidator.WIRE_DIRECTION));
    }

    @Test
    void wireDirectionToOutputPort() {
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("n1", "op.binary"), node("n2", "op.binary")),
                List.of(wire("n1", "out", "n2", "out"))));
        assertTrue(hasCode(diags, GraphValidator.WIRE_DIRECTION));
    }

    // ---------- 5 TYPE_MISMATCH ----------

    @Test
    void typeMismatch() {
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("s", "const.string"), node("r", "entity.root")),
                List.of(wire("s", "out", "r", "scale"))));
        assertTrue(hasCode(diags, GraphValidator.TYPE_MISMATCH));
    }

    // ---------- 6 LOOSE_TYPE (WARNING) ----------

    @Test
    void looseTypeWarning() {
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("v", "var.get"), node("r", "entity.root")),
                List.of(wire("v", "out", "r", "scale"))));
        List<Diagnostic> loose = byCode(diags, GraphValidator.LOOSE_TYPE);
        assertEquals(1, loose.size());
        assertEquals(Diagnostic.Severity.WARNING, loose.get(0).severity());
    }

    // ---------- 7 DUPLICATE_INPUT ----------

    @Test
    void duplicateInput() {
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("c1", "const.number"), node("c2", "const.number"), node("n", "op.binary")),
                List.of(wire("c1", "out", "n", "a"), wire("c2", "out", "n", "a"))));
        assertTrue(hasCode(diags, GraphValidator.DUPLICATE_INPUT));
    }

    // ---------- 8 EXEC_FANOUT ----------

    @Test
    void execFanout() {
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("s1", "exec.set_var"), node("s2", "exec.set_var"), node("s3", "exec.set_var")),
                List.of(wire("s1", "exec_out", "s2", "exec_in"), wire("s1", "exec_out", "s3", "exec_in"))));
        assertTrue(hasCode(diags, GraphValidator.EXEC_FANOUT));
    }

    // ---------- 9 CYCLE ----------

    @Test
    void cycle() {
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("n1", "op.binary"), node("n2", "op.binary")),
                List.of(wire("n1", "out", "n2", "a"), wire("n2", "out", "n1", "a"))));
        List<Diagnostic> cycles = byCode(diags, GraphValidator.CYCLE);
        assertFalse(cycles.isEmpty());
        assertTrue(cycles.get(0).nodeUid().isPresent());
    }

    @Test
    void slotEdgesDoNotCountForCycle() {
        // animate.entry → root.animate 的 SLOT 边不参与环检测：无环
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("r", "entity.root"), node("e", "animate.entry"),
                        node("ra", "ref.animation")),
                List.of(wire("ra", "ref", "e", "ref"), wire("e", "entry", "r", "animate"))));
        assertFalse(hasCode(diags, GraphValidator.CYCLE));
    }

    // ---------- 10 SLOT_KIND ----------

    @Test
    void slotKindViolation() {
        // animate.entry 只能接 entity.root.animate，接 render_controllers 非法
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("r", "entity.root"), node("e", "animate.entry")),
                List.of(wire("e", "entry", "r", "render_controllers"))));
        assertTrue(hasCode(diags, GraphValidator.SLOT_KIND));
    }

    // ---------- 11 REF_MISUSE ----------

    @Test
    void refMisuseAssemblyRefToExpressionSlot() {
        // ref.animation 只能接 animate.entry.ref / rc.condition_entry.rc
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("ra", "ref.animation"), node("c", "rc.condition_entry")),
                List.of(wire("ra", "ref", "c", "condition"))));
        assertTrue(hasCode(diags, GraphValidator.REF_MISUSE));
    }

    @Test
    void refMisuseValueRefToSlot() {
        // ref.geometry 只能接表达式槽 / list.entry.value / material.entry.value
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("g", "ref.geometry"), node("r", "entity.root")),
                List.of(wire("g", "ref", "r", "animate"))));
        assertTrue(hasCode(diags, GraphValidator.REF_MISUSE));
    }

    @Test
    void refGeometryToListEntryValueIsLegal() {
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("g", "ref.geometry"), node("e", "list.entry")),
                List.of(wire("g", "ref", "e", "value"))));
        assertFalse(hasCode(diags, GraphValidator.REF_MISUSE));
    }

    // ---------- 12 ROOT_COUNT ----------

    @Test
    void rootCountZero() {
        GraphLibrary library = lib(GraphKind.CLIENT_ENTITY, graph(List.of(), List.of()));
        assertTrue(hasCode(GraphValidator.validate(library), GraphValidator.ROOT_COUNT));
    }

    @Test
    void rootCountTwo() {
        GraphLibrary library = lib(GraphKind.CLIENT_ENTITY,
                graph(List.of(node("r1", "entity.root"), node("r2", "entity.root")), List.of()));
        assertTrue(hasCode(GraphValidator.validate(library), GraphValidator.ROOT_COUNT));
    }

    // ---------- 13 SUBGRAPH_TARGET ----------

    @Test
    void subgraphTargetMissing() {
        GraphData main = graph(
                List.of(node("r", "entity.root"), node("c", "subgraph.call", opts("subgraph", "missing"))),
                List.of());
        assertTrue(hasCode(GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main)),
                GraphValidator.SUBGRAPH_TARGET));
    }

    @Test
    void subgraphTargetNotASubgraph() {
        // 目标图存在但无 interface
        Map<String, GraphData> graphs = Map.of(
                "root", graph(List.of(node("r", "entity.root"),
                        node("c", "subgraph.call", opts("subgraph", "plain"))), List.of()),
                "plain", graph(List.of(), List.of()));
        assertTrue(hasCode(GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, graphs)),
                GraphValidator.SUBGRAPH_TARGET));
    }

    // ---------- 14 SUBGRAPH_RECURSION ----------

    @Test
    void subgraphRecursionCycle() {
        GraphInterface iface = new GraphInterface(List.of(), GraphInterface.Param.of("result", PortType.FLOAT));
        GraphData a = subgraph(
                List.of(node("call_b", "subgraph.call", opts("subgraph", "b")), node("out", "subgraph.output")),
                List.of(wire("call_b", "result", "out", "result")), iface);
        GraphData b = subgraph(
                List.of(node("call_a", "subgraph.call", opts("subgraph", "a")), node("out", "subgraph.output")),
                List.of(wire("call_a", "result", "out", "result")), iface);
        Map<String, GraphData> graphs = Map.of(
                "root", graph(List.of(node("r", "entity.root")), List.of()),
                "a", a, "b", b);
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, graphs));
        assertTrue(hasCode(diags, GraphValidator.SUBGRAPH_RECURSION));
    }

    @Test
    void subgraphRecursionDepthExceeded() {
        // main → sg0 → sg1 → … → sg33，展开深度 35 > 32
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        GraphInterface iface = new GraphInterface(List.of(), GraphInterface.Param.of("result", PortType.FLOAT));
        int chain = 34;
        for (int i = 0; i < chain; i++) {
            String name = "sg" + i;
            List<NodeInstance> nodes = new ArrayList<>();
            List<Wire> wires = new ArrayList<>();
            nodes.add(node("out", "subgraph.output"));
            if (i + 1 < chain) {
                nodes.add(node("call", "subgraph.call", opts("subgraph", "sg" + (i + 1))));
                wires.add(wire("call", "result", "out", "result"));
            } else {
                nodes.add(node("c", "const.number"));
                wires.add(wire("c", "out", "out", "result"));
            }
            graphs.put(name, subgraph(nodes, wires, iface));
        }
        graphs.put("root", graph(
                List.of(node("r", "entity.root"), node("c0", "subgraph.call", opts("subgraph", "sg0"))),
                List.of(wire("c0", "result", "r", "scale"))));
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, graphs));
        assertTrue(hasCode(diags, GraphValidator.SUBGRAPH_RECURSION));
        // 不应误报环
        assertTrue(byCode(diags, GraphValidator.SUBGRAPH_RECURSION).stream()
                .noneMatch(d -> d.message().contains("环")));
    }

    // ---------- 15 SUBGRAPH_ANCHOR ----------

    @Test
    void subgraphAnchorMissingOutput() {
        GraphInterface iface = new GraphInterface(List.of(), GraphInterface.Param.of("result", PortType.FLOAT));
        Map<String, GraphData> graphs = Map.of(
                "root", graph(List.of(node("r", "entity.root")), List.of()),
                "sg", subgraph(List.of(), List.of(), iface));
        assertTrue(hasCode(GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, graphs)),
                GraphValidator.SUBGRAPH_ANCHOR));
    }

    @Test
    void subgraphAnchorInMainGraph() {
        GraphData main = graph(
                List.of(node("r", "entity.root"), node("out", "subgraph.output")), List.of());
        assertTrue(hasCode(GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main)),
                GraphValidator.SUBGRAPH_ANCHOR));
    }

    // ---------- 16 REF_CONFLICT ----------

    @Test
    void refConflictSameShortNameDifferentIdentifier() {
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("g1", "ref.geometry", opts("short_name", "default", "identifier", "geometry.a")),
                        node("g2", "ref.geometry", opts("short_name", "default", "identifier", "geometry.b"))),
                List.of());
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        assertTrue(hasCode(diags, GraphValidator.REF_CONFLICT));
    }

    @Test
    void refConflictAcrossSubgraph() {
        GraphInterface iface = new GraphInterface(List.of(), GraphInterface.Param.of("result", PortType.FLOAT));
        GraphData sg = subgraph(
                List.of(node("out", "subgraph.output"), node("c", "const.number"),
                        node("g2", "ref.texture", opts("short_name", "default", "path", "textures/b"))),
                List.of(wire("c", "out", "out", "result")), iface);
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("call", "subgraph.call", opts("subgraph", "sg")),
                        node("g1", "ref.texture", opts("short_name", "default", "path", "textures/a"))),
                List.of(wire("call", "result", "r", "scale")));
        Map<String, GraphData> graphs = Map.of("root", main, "sg", sg);
        assertTrue(hasCode(GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, graphs)),
                GraphValidator.REF_CONFLICT));
    }

    @Test
    void refConflictDerivedSanitizeCollision() {
        // "textures/a/b" 与 "textures.a.b" 派生同名 → 冲突（D5）
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("t1", "ref.texture", opts("path", "textures/a/b")),
                        node("t2", "ref.texture", opts("path", "textures.a.b"))),
                List.of());
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        assertTrue(hasCode(diags, GraphValidator.REF_CONFLICT));
    }

    @Test
    void refConflictAnimationAcShareNamespace() {
        // ref.animation 与 ref.ac 同发 animations 表（D6），同短名不同标识 → 冲突
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("a1", "ref.animation", opts("short_name", "x", "identifier", "animation.a")),
                        node("a2", "ref.ac", opts("short_name", "x", "identifier", "controller.animation.b"))),
                List.of());
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        assertTrue(hasCode(diags, GraphValidator.REF_CONFLICT));
    }

    @Test
    void invalidExplicitShortName() {
        // 显式短名含非法字符（会发射为 molang 成员访问的类别）→ INVALID_SHORT_NAME
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("t1", "ref.texture", opts("short_name", "My-Skin", "path", "textures/a"))),
                List.of());
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        assertTrue(hasCode(diags, GraphValidator.INVALID_SHORT_NAME));
    }

    @Test
    void emptyEffectiveShortName() {
        // short_name 与标识符均空 → 有效短名空 → INVALID_SHORT_NAME
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("g1", "ref.geometry", opts("short_name", "", "identifier", ""))),
                List.of());
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        assertTrue(hasCode(diags, GraphValidator.INVALID_SHORT_NAME));
        assertFalse(hasCode(diags, GraphValidator.REF_CONFLICT));
    }

    @Test
    void explicitShortNameOnAnimationSkipsMolangCheck() {
        // animation/ac 短名不进 molang（JSON 键），显式值含大写不查 INVALID_SHORT_NAME
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("a1", "ref.animation", opts("short_name", "Walk", "identifier", "animation.a"))),
                List.of());
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        assertFalse(hasCode(diags, GraphValidator.INVALID_SHORT_NAME));
    }

    // ---------- 17 UNCONNECTED_INPUT ----------

    @Test
    void unconnectedInput() {
        GraphData main = graph(
                List.of(node("r", "entity.root", Map.of(), opts("scale_x", 1, "scale_y", 1, "scale_z", 1)),
                        node("e", "animate.entry")),
                List.of(wire("e", "entry", "r", "animate")));
        List<Diagnostic> diags = validateGraph(main);
        List<Diagnostic> unconnected = byCode(diags, GraphValidator.UNCONNECTED_INPUT);
        // e.ref（ANY 无默认、无内联）未连接
        assertTrue(unconnected.stream().anyMatch(d -> d.nodeUid().orElse("").equals("e")));
    }

    @Test
    void unconnectedInputUnreachableNodeNotReported() {
        // 不可达节点不报 UNCONNECTED_INPUT
        GraphData main = graph(
                List.of(node("r", "entity.root", Map.of(), opts("scale_x", 1, "scale_y", 1, "scale_z", 1)),
                        node("e", "animate.entry")),
                List.of());
        List<Diagnostic> diags = validateGraph(main);
        assertTrue(byCode(diags, GraphValidator.UNCONNECTED_INPUT).stream()
                .noneMatch(d -> d.nodeUid().orElse("").equals("e")));
    }

    // ---------- 18 ORPHAN_CHAIN (WARNING) ----------

    @Test
    void orphanChain() {
        GraphData main = graph(
                List.of(node("r", "entity.root", Map.of(), opts("scale_x", 1, "scale_y", 1, "scale_z", 1)),
                        node("s", "exec.set_var", opts("root", "temp", "name", "temp.t"))),
                List.of());
        List<Diagnostic> diags = validateGraph(main);
        List<Diagnostic> orphans = byCode(diags, GraphValidator.ORPHAN_CHAIN);
        assertEquals(1, orphans.size());
        assertEquals(Diagnostic.Severity.WARNING, orphans.get(0).severity());
        assertEquals("s", orphans.get(0).nodeUid().orElseThrow());
    }

    @Test
    void connectedExecChainIsNotOrphan() {
        GraphData main = graph(
                List.of(node("r", "entity.root", Map.of(), opts("scale_x", 1, "scale_y", 1, "scale_z", 1)),
                        node("s", "exec.set_var", opts("root", "temp", "name", "temp.t"))),
                List.of(wire("s", "exec_out", "r", "initialize")));
        assertFalse(hasCode(validateGraph(main), GraphValidator.ORPHAN_CHAIN));
    }

    // ---------- 19 UNDECLARED_VARIABLE (WARNING) ----------

    @Test
    void undeclaredVariable() {
        GraphData main = graph(
                List.of(node("v", "var.get", opts("name", "variable.missing"))), List.of());
        List<Diagnostic> diags = validateGraph(main);
        List<Diagnostic> undeclared = byCode(diags, GraphValidator.UNDECLARED_VARIABLE);
        assertEquals(1, undeclared.size());
        assertEquals(Diagnostic.Severity.WARNING, undeclared.get(0).severity());
    }

    @Test
    void declaredVariableIsFine() {
        GraphData main = graph(
                List.of(node("v", "var.get", opts("name", "variable.foo"))),
                List.of(), List.of(VariableDecl.of("foo", PortType.FLOAT)));
        assertFalse(hasCode(validateGraph(main), GraphValidator.UNDECLARED_VARIABLE));
    }

    // ---------- 完整合法图：零 ERROR ----------

    @Test
    void legalClientEntityLibraryHasNoErrors() {
        // 子图 sg：input x → output result
        GraphInterface iface = new GraphInterface(
                List.of(GraphInterface.Param.of("x", PortType.FLOAT)),
                GraphInterface.Param.of("result", PortType.FLOAT));
        GraphData sg = subgraph(
                List.of(node("si", "subgraph.input"), node("so", "subgraph.output")),
                List.of(wire("si", "x", "so", "result")), iface);

        // 主图：entity.root + scale 子图调用 + initialize exec 链 + animate.entry
        NodeInstance root = node("r", "entity.root", Map.of(),
                opts("scale_x", 1, "scale_y", 1, "scale_z", 1));
        NodeInstance call = node("sc", "subgraph.call", opts("subgraph", "sg"));
        NodeInstance arg = node("c1", "const.number", opts("value", 2));
        NodeInstance setVar = node("sv", "exec.set_var", opts("root", "variable", "name", "variable.foo"));
        NodeInstance animRef = node("ra", "ref.animation",
                opts("short_name", "walk", "identifier", "animation.example.walk"));
        NodeInstance entry = node("ae", "animate.entry");

        GraphData main = graph(
                List.of(root, call, arg, setVar, animRef, entry),
                List.of(
                        wire("sc", "result", "r", "scale"),
                        wire("c1", "out", "sc", "x"),
                        wire("sv", "exec_out", "r", "initialize"),
                        wire("ra", "ref", "ae", "ref"),
                        wire("ae", "entry", "r", "animate")),
                List.of(VariableDecl.of("foo", PortType.FLOAT)));

        Map<String, GraphData> graphs = new LinkedHashMap<>();
        graphs.put("root", main);
        graphs.put("sg", sg);

        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, graphs));
        assertFalse(GraphValidator.hasErrors(diags),
                "合法图不应有 ERROR，实际：" + diags.stream()
                        .filter(d -> d.severity() == Diagnostic.Severity.ERROR).toList());
    }

    // ---------- 边界：未知类型不 NPE ----------

    @Test
    void unknownTypeWithWiresDoesNotThrow() {
        GraphData main = graph(
                List.of(node("n1", "no.such.type"), node("c1", "const.number")),
                List.of(wire("c1", "out", "n1", "a"), wire("n1", "out", "c1", "out")));
        List<Diagnostic> diags = validateGraph(main);
        assertTrue(hasCode(diags, GraphValidator.UNKNOWN_NODE_TYPE));
    }

    @Test
    void duplicateOptionsVarargs() {
        // 构造辅助自检：opts 支持 String/Number/Boolean
        Map<String, JsonElement> o = opts("a", "x", "b", 1, "c", true);
        assertEquals("x", o.get("a").getAsString());
        assertEquals(1, o.get("b").getAsInt());
        assertTrue(o.get("c").getAsBoolean());
    }

    @Test
    void acLibrarySlotWhitelist() {
        // ac.root.states ← ac.state；ac.state.transitions ← ac.transition 合法
        GraphData main = graph(
                List.of(node("r", "ac.root"), node("s", "ac.state"), node("t", "ac.transition")),
                List.of(wire("s", "state", "r", "states"), wire("t", "transition", "s", "transitions")));
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.ANIMATION_CONTROLLER, main));
        assertFalse(hasCode(diags, GraphValidator.SLOT_KIND));
        assertFalse(hasCode(diags, GraphValidator.ROOT_COUNT));
    }
}
