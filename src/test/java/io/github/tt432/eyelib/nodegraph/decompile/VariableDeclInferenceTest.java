package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import io.github.tt432.eyelib.nodegraph.Wire;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link VariableDeclInference} 单测：写入来源类型归并（连线静态类型 / 行内字面量）、
 * 冲突与未知收口 UNKNOWN、点分成员名 OBJECT 父链补齐。
 */
class VariableDeclInferenceTest {

    private static NodeInstance node(String uid, String type) {
        return NodeInstance.of(uid, type, 0, 0);
    }

    private static NodeInstance node(String uid, String type, Map<String, JsonElement> opts) {
        return new NodeInstance(uid, type, 0, 0, opts, Map.of());
    }

    private static NodeInstance constNode(String uid, String type, JsonElement value) {
        return new NodeInstance(uid, type, 0, 0, Map.of("value", value), Map.of());
    }

    private static Wire wire(String from, String fromPort, String to, String toPort) {
        return new Wire(new PortRef(from, fromPort), new PortRef(to, toPort));
    }

    /** variable.s = &lt;wired const&gt;：target 接 variable.in，value 接 const.out。 */
    private static PortType inferFromConst(NodeInstance constNode) {
        List<NodeInstance> nodes = List.of(
                node("v", "variable", Map.of("name", new JsonPrimitive("s"))),
                node("set", "exec.set_var"),
                constNode);
        List<Wire> wires = List.of(
                wire("set", "target", "v", "in"),
                wire(constNode.uid(), "out", "set", "value"));
        return VariableDeclInference.inferWrittenTypes(nodes, wires).get("s");
    }

    @Test
    void wiredConstTypesPropagate() {
        assertEquals(PortType.STRING, inferFromConst(constNode("c", "const.string", new JsonPrimitive("abc"))));
        assertEquals(PortType.FLOAT, inferFromConst(constNode("c", "const.number", new JsonPrimitive(1.5))));
        assertEquals(PortType.INT, inferFromConst(constNode("c", "const.int", new JsonPrimitive(3))));
        assertEquals(PortType.BOOL, inferFromConst(constNode("c", "const.bool", new JsonPrimitive(true))));
    }

    @Test
    void inlineLiteralInfersString() {
        List<NodeInstance> nodes = List.of(
                node("v", "variable", Map.of("name", new JsonPrimitive("s"))),
                new NodeInstance("set", "exec.set_var", 0, 0, Map.of(),
                        Map.of("value", new JsonPrimitive("abc"))));
        List<Wire> wires = List.of(wire("set", "target", "v", "in"));
        assertEquals(PortType.STRING, VariableDeclInference.inferWrittenTypes(nodes, wires).get("s"));
    }

    @Test
    void unknownSourcesYieldNoEntry() {
        // query.call out 静态 ANY → 无把握
        List<NodeInstance> nodes = List.of(
                node("v", "variable", Map.of("name", new JsonPrimitive("s"))),
                node("set", "exec.set_var"),
                node("q", "query.call", Map.of("function", new JsonPrimitive("query.anim_time"))));
        List<Wire> wires = List.of(
                wire("set", "target", "v", "in"),
                wire("q", "out", "set", "value"));
        assertTrue(VariableDeclInference.inferWrittenTypes(nodes, wires).isEmpty());

        // 无写入 → 无条目
        List<NodeInstance> readOnly = List.of(
                node("v", "variable", Map.of("name", new JsonPrimitive("s"))));
        assertTrue(VariableDeclInference.inferWrittenTypes(readOnly, List.of()).isEmpty());
    }

    @Test
    void conflictingWritesYieldNoEntry() {
        List<NodeInstance> nodes = List.of(
                node("v", "variable", Map.of("name", new JsonPrimitive("s"))),
                node("set1", "exec.set_var"),
                node("set2", "exec.set_var"),
                constNode("c1", "const.string", new JsonPrimitive("a")),
                constNode("c2", "const.number", new JsonPrimitive(1.5)));
        List<Wire> wires = List.of(
                wire("set1", "target", "v", "in"),
                wire("c1", "out", "set1", "value"),
                wire("set2", "target", "v", "in"),
                wire("c2", "out", "set2", "value"));
        // string vs float 冲突 → ANY → 归并为无把握（声明落 UNKNOWN）
        assertTrue(VariableDeclInference.inferWrittenTypes(nodes, wires).isEmpty());
    }

    @Test
    void conflictIsSticky() {
        // 冲突后再写同型不回翻（strictMerge 粘性，区别于显示层 merge 的让位）
        List<NodeInstance> nodes = List.of(
                node("v", "variable", Map.of("name", new JsonPrimitive("s"))),
                node("set1", "exec.set_var"),
                node("set2", "exec.set_var"),
                node("set3", "exec.set_var"),
                constNode("c1", "const.string", new JsonPrimitive("a")),
                constNode("c2", "const.number", new JsonPrimitive(1.5)),
                constNode("c3", "const.string", new JsonPrimitive("b")));
        List<Wire> wires = List.of(
                wire("set1", "target", "v", "in"),
                wire("c1", "out", "set1", "value"),
                wire("set2", "target", "v", "in"),
                wire("c2", "out", "set2", "value"),
                wire("set3", "target", "v", "in"),
                wire("c3", "out", "set3", "value"));
        assertTrue(VariableDeclInference.inferWrittenTypes(nodes, wires).isEmpty());
    }

    @Test
    void sameTypeWritesMerge() {
        List<NodeInstance> nodes = List.of(
                node("v", "variable", Map.of("name", new JsonPrimitive("s"))),
                node("set1", "exec.set_var"),
                node("set2", "exec.set_var"),
                constNode("c1", "const.int", new JsonPrimitive(1)),
                constNode("c2", "const.number", new JsonPrimitive(0.5)));
        List<Wire> wires = List.of(
                wire("set1", "target", "v", "in"),
                wire("c1", "out", "set1", "value"),
                wire("set2", "target", "v", "in"),
                wire("c2", "out", "set2", "value"));
        // number 子型混合 → FLOAT
        assertEquals(PortType.FLOAT, VariableDeclInference.inferWrittenTypes(nodes, wires).get("s"));
    }

    @Test
    void declTypeFallsBackToUnknown() {
        assertEquals(PortType.UNKNOWN, VariableDeclInference.declType(Map.of(), "x"));
        assertEquals(PortType.STRING,
                VariableDeclInference.declType(Map.of("x", PortType.STRING), "x"));
    }

    @Test
    void objectParentsCompleted() {
        List<VariableDecl> out = VariableDeclInference.completeObjectParents(List.of(
                VariableDecl.of("qpptaw.r", PortType.INT),
                VariableDecl.of("qpptaw.x", PortType.INT),
                VariableDecl.of("plain", PortType.FLOAT)));
        var byName = new java.util.HashMap<String, PortType>();
        out.forEach(d -> byName.put(d.name(), d.type()));
        assertEquals(PortType.OBJECT, byName.get("qpptaw"));
        assertEquals(PortType.INT, byName.get("qpptaw.r"));
        assertEquals(PortType.FLOAT, byName.get("plain"));
        assertEquals(4, out.size());
    }

    @Test
    void nestedParentsCompletedAndExistingPreserved() {
        List<VariableDecl> out = VariableDeclInference.completeObjectParents(List.of(
                VariableDecl.of("a.b.c", PortType.STRING),
                VariableDecl.of("a", PortType.FLOAT)));
        var byName = new java.util.HashMap<String, PortType>();
        out.forEach(d -> byName.put(d.name(), d.type()));
        assertEquals(PortType.FLOAT, byName.get("a"), "已有父声明不覆盖");
        assertEquals(PortType.OBJECT, byName.get("a.b"));
        assertEquals(3, out.size());
    }
}
