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
        // animate.entry 只能接 entity.root.animate / ac.state.animations；接 rc.root.textures 非法（应为 list.entry）
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("r", "rc.root"), node("e", "animate.entry")),
                List.of(wire("e", "entry", "r", "textures"))));
        assertTrue(hasCode(diags, GraphValidator.SLOT_KIND));
    }

    @Test
    void renderControllersIsNoLongerSlot() {
        // v4：entity.root.render_controllers 是 RC_REF inMulti（SLOT_WHITELIST 无条目）；
        // SLOT 条目接入 → 类型不兼容，不再是 SLOT_KIND
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("r", "entity.root"), node("e", "animate.entry")),
                List.of(wire("e", "entry", "r", "render_controllers"))));
        assertFalse(hasCode(diags, GraphValidator.SLOT_KIND));
        assertTrue(hasCode(diags, GraphValidator.TYPE_MISMATCH));
    }

    // ---------- 11 REF_MISUSE ----------

    @Test
    void refMisuseAssemblyRefToExpressionSlot() {
        // ref.animation 只能接 animate.entry.ref / entity.root 动画声明端口（v4）
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("ra", "ref.animation"), node("r", "entity.root")),
                List.of(wire("ra", "ref", "r", "scale"))));
        assertTrue(hasCode(diags, GraphValidator.REF_MISUSE));
    }

    @Test
    void refMisuseRefRcToNonMountPort() {
        // ref.rc 只能接 entity.root.render_controllers（v4）
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("rc", "ref.rc"), node("r", "entity.root")),
                List.of(wire("rc", "ref", "r", "scale"))));
        assertTrue(hasCode(diags, GraphValidator.REF_MISUSE));
    }

    @Test
    void refMisuseRcRootControllerToNonMountPort() {
        // rc.root.controller 只能接 entity.root.render_controllers（v4）
        List<Diagnostic> diags = validateGraph(graph(
                List.of(node("rcr", "rc.root"), node("r", "entity.root")),
                List.of(wire("rcr", "controller", "r", "scale"))));
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
        // v4：REF_CONFLICT 范围 = DeclarationTables 派生集合（RC 锚点）
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("rc", "ref.rc", opts("identifier", "controller.render.a")),
                        node("g1", "ref.geometry", opts("short_name", "default", "identifier", "geometry.a")),
                        node("g2", "ref.geometry", opts("short_name", "default", "identifier", "geometry.b"))),
                List.of(wire("rc", "ref", "r", "render_controllers"),
                        wire("g1", "ref", "rc", "decl_geometries"),
                        wire("g2", "ref", "rc", "decl_geometries")));
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        assertTrue(hasCode(diags, GraphValidator.REF_CONFLICT));
    }

    @Test
    void refConflictSkipsUnwiredAndSubgraphRefs() {
        // 未接 RC 锚点的主图 ref / 子图 ref 不进声明表（v4），不参与 REF_CONFLICT
        GraphInterface iface = new GraphInterface(List.of(), GraphInterface.Param.of("result", PortType.FLOAT));
        GraphData sg = subgraph(
                List.of(node("out", "subgraph.output"), node("c", "const.number"),
                        node("g3", "ref.geometry", opts("short_name", "default", "identifier", "geometry.c"))),
                List.of(wire("c", "out", "out", "result")), iface);
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("rc", "ref.rc", opts("identifier", "controller.render.a")),
                        node("call", "subgraph.call", opts("subgraph", "sg")),
                        node("g1", "ref.geometry", opts("short_name", "default", "identifier", "geometry.a")),
                        node("g2", "ref.geometry", opts("short_name", "other", "identifier", "geometry.b"))),
                List.of(wire("call", "result", "r", "scale"),
                        wire("rc", "ref", "r", "render_controllers"),
                        wire("g1", "ref", "rc", "decl_geometries")));
        Map<String, GraphData> graphs = Map.of("root", main, "sg", sg);
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, graphs));
        assertFalse(hasCode(diags, GraphValidator.REF_CONFLICT));
    }

    @Test
    void refConflictDerivedSanitizeCollision() {
        // "textures/a/b" 与 "textures.a.b" 派生同名 → 冲突（D5）
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("rc", "ref.rc", opts("identifier", "controller.render.a")),
                        node("t1", "ref.texture", opts("path", "textures/a/b")),
                        node("t2", "ref.texture", opts("path", "textures.a.b"))),
                List.of(wire("rc", "ref", "r", "render_controllers"),
                        wire("t1", "ref", "rc", "decl_textures"),
                        wire("t2", "ref", "rc", "decl_textures")));
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
                List.of(wire("a1", "ref", "r", "animations"),
                        wire("a2", "ref", "r", "animation_controllers")));
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
                        node("s", "exec.set_temp", opts("name", "temp.t"))),
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
                        node("s", "exec.set_temp", opts("name", "temp.t"))),
                List.of(wire("s", "exec_out", "r", "initialize")));
        assertFalse(hasCode(validateGraph(main), GraphValidator.ORPHAN_CHAIN));
    }

    // ---------- 19 UNDECLARED_VARIABLE (WARNING) ----------

    @Test
    void undeclaredVariable() {
        GraphData main = graph(
                List.of(node("v", "variable", opts("name", "missing"))), List.of());
        List<Diagnostic> diags = validateGraph(main);
        List<Diagnostic> undeclared = byCode(diags, GraphValidator.UNDECLARED_VARIABLE);
        assertEquals(1, undeclared.size());
        assertEquals(Diagnostic.Severity.WARNING, undeclared.get(0).severity());
    }

    @Test
    void declaredVariableIsFine() {
        GraphData main = graph(
                List.of(node("v", "variable", opts("name", "foo"))),
                List.of(), List.of(VariableDecl.of("foo", PortType.FLOAT)));
        assertFalse(hasCode(validateGraph(main), GraphValidator.UNDECLARED_VARIABLE));
    }

    // ---------- 20 SET_TARGET_NOT_VARIABLE ----------

    @Test
    void setVarTargetUnconnectedIsError() {
        GraphData main = graph(
                List.of(node("r", "entity.root", Map.of(), opts("scale_x", 1, "scale_y", 1, "scale_z", 1)),
                        node("s", "exec.set_var")),
                List.of(wire("s", "exec_out", "r", "initialize")));
        assertTrue(hasCode(validateGraph(main), GraphValidator.SET_TARGET_NOT_VARIABLE));
    }

    @Test
    void setVarTargetFromConstantIsError() {
        GraphData main = graph(
                List.of(node("r", "entity.root", Map.of(), opts("scale_x", 1, "scale_y", 1, "scale_z", 1)),
                        node("s", "exec.set_var"),
                        node("c", "const.number")),
                List.of(wire("s", "exec_out", "r", "initialize"),
                        wire("s", "target", "c", "in")));
        assertTrue(hasCode(validateGraph(main), GraphValidator.SET_TARGET_NOT_VARIABLE));
    }

    @Test
    void setVarTargetFromVariableNodeIsFine() {
        GraphData main = graph(
                List.of(node("r", "entity.root", Map.of(), opts("scale_x", 1, "scale_y", 1, "scale_z", 1)),
                        node("s", "exec.set_var"),
                        node("v", "variable", opts("name", "foo"))),
                List.of(wire("s", "exec_out", "r", "initialize"),
                        wire("s", "target", "v", "in")),
                List.of(VariableDecl.of("foo", PortType.FLOAT)));
        assertFalse(hasCode(validateGraph(main), GraphValidator.SET_TARGET_NOT_VARIABLE));
    }

    // ---------- 21 REF_NOT_CONNECTED (WARNING，仅 CLIENT_ENTITY) ----------

    @Test
    void declarationRefNotConnected() {
        // v4 口径一：主图 ref.geometry 带标识符但未接入任何 RC 锚点 → WARNING
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("g1", "ref.geometry", opts("identifier", "geometry.a"))),
                List.of());
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        List<Diagnostic> warns = byCode(diags, GraphValidator.REF_NOT_CONNECTED);
        assertEquals(1, warns.size());
        assertEquals(Diagnostic.Severity.WARNING, warns.get(0).severity());
        assertEquals("g1", warns.get(0).nodeUid().orElseThrow());
    }

    @Test
    void declarationRefWiredToAnchorIsFine() {
        // ref.geometry 接 rc.root 声明端口 → 入派生集合，不报
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("rcr", "rc.root", opts("identifier", "controller.render.a")),
                        node("g1", "ref.geometry", opts("identifier", "geometry.a"))),
                List.of(wire("g1", "ref", "rcr", "decl_geometries"),
                        wire("rcr", "controller", "r", "render_controllers")));
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        assertFalse(hasCode(diags, GraphValidator.REF_NOT_CONNECTED));
    }

    @Test
    void animateEntryOnlyRefStillNotDeclared() {
        // 仅接 animate.entry 不进声明表：消息需点明该情形
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("e", "animate.entry"),
                        node("a1", "ref.animation", opts("identifier", "animation.a"))),
                List.of(wire("a1", "ref", "e", "ref"), wire("e", "entry", "r", "animate")));
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        List<Diagnostic> warns = byCode(diags, GraphValidator.REF_NOT_CONNECTED);
        assertEquals(1, warns.size());
        assertTrue(warns.get(0).message().contains("animate.entry"));
    }

    @Test
    void placeholderRefNotReported() {
        // 标识符选项无实例值的占位 ref 不报（UNKNOWN_REFERENCE 已覆盖）；接入锚点的 ref 也不报
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("rc", "ref.rc", opts("identifier", "controller.render.a")),
                        node("g1", "ref.geometry", opts("short_name", "walk")),
                        node("g2", "ref.geometry", opts("identifier", "geometry.a"))),
                List.of(wire("rc", "ref", "r", "render_controllers"),
                        wire("g2", "ref", "rc", "decl_geometries")));
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        assertFalse(hasCode(diags, GraphValidator.REF_NOT_CONNECTED));
    }

    @Test
    void refRcNotMounted() {
        // v4 口径三：ref.rc 的 ref 未接 entity.root.render_controllers → WARNING
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.a"))),
                List.of());
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        List<Diagnostic> warns = byCode(diags, GraphValidator.REF_NOT_CONNECTED);
        assertEquals(1, warns.size());
        assertEquals("rc1", warns.get(0).nodeUid().orElseThrow());
    }

    @Test
    void refRcMountedDirectlyIsFine() {
        // v4：ref.rc 直连 entity.root.render_controllers（无 rc.condition_entry）
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.a"))),
                List.of(wire("rc1", "ref", "r", "render_controllers")));
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        assertFalse(hasCode(diags, GraphValidator.REF_NOT_CONNECTED));
    }

    @Test
    void rcRootControllerNotConnected() {
        // v4 口径二：内联 rc.root 的 controller 未接 entity.root.render_controllers → WARNING
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("rcr", "rc.root", opts("identifier", "controller.render.a"))),
                List.of());
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        List<Diagnostic> warns = byCode(diags, GraphValidator.REF_NOT_CONNECTED);
        assertEquals(1, warns.size());
        assertEquals("rcr", warns.get(0).nodeUid().orElseThrow());
    }

    // ---------- 22 DUPLICATE_RC_ID (ERROR，仅 CLIENT_ENTITY) ----------

    @Test
    void duplicateRcId() {
        // 主图两个内联 rc.root 同 identifier → ERROR
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("rcr1", "rc.root", opts("identifier", "controller.render.a")),
                        node("rcr2", "rc.root", opts("identifier", "controller.render.a"))),
                List.of(wire("rcr1", "controller", "r", "render_controllers"),
                        wire("rcr2", "controller", "r", "render_controllers")));
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        List<Diagnostic> errors = byCode(diags, GraphValidator.DUPLICATE_RC_ID);
        assertEquals(1, errors.size());
        assertEquals(Diagnostic.Severity.ERROR, errors.get(0).severity());
    }

    @Test
    void distinctRcIdsAreFine() {
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("rcr1", "rc.root", opts("identifier", "controller.render.a")),
                        node("rcr2", "rc.root", opts("identifier", "controller.render.b"))),
                List.of(wire("rcr1", "controller", "r", "render_controllers"),
                        wire("rcr2", "controller", "r", "render_controllers")));
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        assertFalse(hasCode(diags, GraphValidator.DUPLICATE_RC_ID));
    }

    @Test
    void sameIdRefRcIsNotDuplicate() {
        // DUPLICATE_RC_ID 只查内联 rc.root；外部 ref.rc 同 id 不报
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("rc1", "ref.rc", opts("identifier", "controller.render.a")),
                        node("rc2", "ref.rc", opts("identifier", "controller.render.a"))),
                List.of(wire("rc1", "ref", "r", "render_controllers"),
                        wire("rc2", "ref", "r", "render_controllers")));
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, main));
        assertFalse(hasCode(diags, GraphValidator.DUPLICATE_RC_ID));
    }

    @Test
    void subgraphDeclarationRefHintsMoveToMain() {
        GraphInterface iface = new GraphInterface(List.of(), GraphInterface.Param.of("result", PortType.FLOAT));
        GraphData sg = subgraph(
                List.of(node("out", "subgraph.output"), node("c", "const.number"),
                        node("t1", "ref.texture", opts("path", "textures/a"))),
                List.of(wire("c", "out", "out", "result")), iface);
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("call", "subgraph.call", opts("subgraph", "sg"))),
                List.of(wire("call", "result", "r", "scale")));
        Map<String, GraphData> graphs = Map.of("root", main, "sg", sg);
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.CLIENT_ENTITY, graphs));
        List<Diagnostic> warns = byCode(diags, GraphValidator.REF_NOT_CONNECTED);
        assertEquals(1, warns.size());
        assertTrue(warns.get(0).message().contains("移至主图"));
        assertEquals("t1", warns.get(0).nodeUid().orElseThrow());
    }

    @Test
    void rcLibraryRefsNotReportedNotConnected() {
        // RC/AC 库中的 ref.*（接表达式槽的 VALUE_REFS 用法）不受影响
        GraphData main = graph(
                List.of(node("r", "rc.root"),
                        node("m1", "ref.material", opts("short_name", "default"))),
                List.of());
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.RENDER_CONTROLLER, main));
        assertFalse(hasCode(diags, GraphValidator.REF_NOT_CONNECTED));
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
        NodeInstance setVar = node("sv", "exec.set_var");
        NodeInstance setTarget = node("svt", "variable", opts("name", "foo"));
        NodeInstance animRef = node("ra", "ref.animation",
                opts("short_name", "walk", "identifier", "animation.example.walk"));
        NodeInstance entry = node("ae", "animate.entry");

        GraphData main = graph(
                List.of(root, call, arg, setVar, setTarget, animRef, entry),
                List.of(
                        wire("sc", "result", "r", "scale"),
                        wire("c1", "out", "sc", "x"),
                        wire("sv", "exec_out", "r", "initialize"),
                        wire("sv", "target", "svt", "in"),
                        wire("ra", "ref", "ae", "ref"),
                        wire("ra", "ref", "r", "animations"),
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

    // ---------- v6：条目链（规格 §2.3） ----------

    @Test
    void listMultiHeadReported() {
        GraphData main = graph(
                List.of(node("root", "rc.root", opts("identifier", "controller.render.a")),
                        node("e1", "list.entry"), node("e2", "list.entry")),
                List.of(wire("e1", "entry", "root", "textures"),
                        wire("e2", "entry", "root", "textures")));
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.RENDER_CONTROLLER, main));
        assertTrue(hasCode(diags, GraphValidator.LIST_MULTI_HEAD));
    }

    @Test
    void listCycleReported() {
        // 脱链环：e1.next←e2、e2.next←e1，无链头
        GraphData main = graph(
                List.of(node("root", "rc.root", opts("identifier", "controller.render.a")),
                        node("e1", "list.entry"), node("e2", "list.entry")),
                List.of(wire("e2", "entry", "e1", "next"),
                        wire("e1", "entry", "e2", "next")));
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.RENDER_CONTROLLER, main));
        assertTrue(hasCode(diags, GraphValidator.LIST_CYCLE));
    }

    @Test
    void entryOrphanReportedOnlyForUnclaimed() {
        GraphData main = graph(
                List.of(node("root", "rc.root", opts("identifier", "controller.render.a")),
                        node("e1", "list.entry"), node("e2", "list.entry"), node("e3", "list.entry")),
                List.of(wire("e1", "entry", "root", "textures"),
                        wire("e2", "entry", "e1", "next")));
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.RENDER_CONTROLLER, main));
        // e3 孤儿（告警）；e1/e2 在链上 → 全图只此 1 条
        assertEquals(1, diags.stream().filter(d -> d.code().equals(GraphValidator.ENTRY_ORPHAN)
                && d.nodeUid().equals(java.util.Optional.of("e3"))).count());
    }

    @Test
    void wellFormedChainPasses() {
        GraphData main = graph(
                List.of(node("root", "rc.root", opts("identifier", "controller.render.a")),
                        node("e1", "list.entry"), node("e2", "list.entry")),
                List.of(wire("e1", "entry", "root", "textures"),
                        wire("e2", "entry", "e1", "next")));
        List<Diagnostic> diags = GraphValidator.validate(lib(GraphKind.RENDER_CONTROLLER, main));
        assertFalse(hasCode(diags, GraphValidator.LIST_MULTI_HEAD));
        assertFalse(hasCode(diags, GraphValidator.LIST_CYCLE));
        assertFalse(hasCode(diags, GraphValidator.ENTRY_ORPHAN));
        assertFalse(hasCode(diags, GraphValidator.SLOT_KIND));
    }
}
