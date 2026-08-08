package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.Wire;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.opts;
import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link VariableInliner} 单测：只断言可观测契约——内联后节点/连线/常量形态与
 * 「不满足条件即原样保留」的负例。
 */
class VariableInlinerTest {

    private static NodeInstance node(String uid, String type) {
        return new NodeInstance(uid, type, 0, 0, Map.of(), Map.of());
    }

    private static NodeInstance node(String uid, String type, Map<String, JsonElement> options) {
        return new NodeInstance(uid, type, 0, 0, options, Map.of());
    }

    private static Wire wire(String fromNode, String fromPort, String toNode, String toPort) {
        return new Wire(new PortRef(fromNode, fromPort), new PortRef(toNode, toPort));
    }

    private static boolean hasNode(VariableInliner.Result r, String type) {
        return r.nodes().stream().anyMatch(n -> n.type().equals(type));
    }

    private static boolean hasWire(VariableInliner.Result r, String fromNode, String fromPort,
                                   String toNode, String toPort) {
        return r.wires().stream().anyMatch(w -> w.from().node().equals(fromNode)
                && w.from().port().equals(fromPort)
                && w.to().node().equals(toNode) && w.to().port().equals(toPort));
    }

    /** temp：单写单读 + 先写后读 → 内联（值直连、set/get 删除、exec 旁路）。 */
    @Test
    void tempSingleWriteSingleReadInlined() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("c", "const.int", opts("value", 5)),
                node("s", "exec.set_temp", opts("name", "temp.t0")),
                node("g", "temp.get", opts("name", "temp.t0")),
                node("r", "exec.return")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("c", "out", "s", "value"),
                wire("s", "exec_out", "r", "exec_in"),
                wire("g", "out", "r", "value")));

        VariableInliner.Result r = VariableInliner.inline(nodes, wires);

        assertFalse(hasNode(r, "exec.set_temp"));
        assertFalse(hasNode(r, "temp.get"));
        assertTrue(hasWire(r, "c", "out", "r", "value"));
        assertTrue(DecompileTestSupport.hasCode(r.diagnostics(), VariableInliner.INLINED_VARIABLE));
    }

    /** variable 形态：set_var + variable 节点 + 中间 exec 旁路（a→s→r 变 a→r）。 */
    @Test
    void variableFormInlinedWithExecBypass() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("v", "variable", opts("name", "hp")),
                node("s", "exec.set_var"),
                node("c", "const.number", opts("value", 1.5)),
                node("a", "exec.call", opts("function", "query.foo", "arg_count", 0)),
                node("r", "exec.return")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("v", "out", "s", "target"),
                wire("c", "out", "s", "value"),
                wire("a", "exec_out", "s", "exec_in"),
                wire("s", "exec_out", "r", "exec_in"),
                wire("v", "out", "r", "value")));

        VariableInliner.Result r = VariableInliner.inline(nodes, wires);

        assertFalse(hasNode(r, "variable"));
        assertFalse(hasNode(r, "exec.set_var"));
        assertTrue(hasWire(r, "a", "exec_out", "r", "exec_in"));
        assertTrue(hasWire(r, "c", "out", "r", "value"));
    }

    /** 多次读取 → 不内联。 */
    @Test
    void multiReadNotInlined() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("s", "exec.set_temp", opts("name", "temp.t0")),
                node("g", "temp.get", opts("name", "temp.t0")),
                node("r1", "exec.return"),
                node("r2", "exec.return")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "exec_out", "r1", "exec_in"),
                wire("r1", "exec_out", "r2", "exec_in"),
                wire("g", "out", "r1", "value"),
                wire("g", "out", "r2", "value")));

        VariableInliner.Result r = VariableInliner.inline(nodes, wires);

        assertTrue(hasNode(r, "exec.set_temp"));
        assertTrue(hasNode(r, "temp.get"));
        assertFalse(DecompileTestSupport.hasCode(r.diagnostics(), VariableInliner.INLINED_VARIABLE));
    }

    /** 读取点在 exec 流上先于写入（不可达）→ 不内联。 */
    @Test
    void readBeforeWriteNotInlined() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("s", "exec.set_temp", opts("name", "temp.t0")),
                node("g", "temp.get", opts("name", "temp.t0")),
                node("r1", "exec.return"),
                node("r2", "exec.return")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "exec_out", "r2", "exec_in"),
                wire("g", "out", "r1", "value")));

        VariableInliner.Result r = VariableInliner.inline(nodes, wires);

        assertTrue(hasNode(r, "exec.set_temp"));
        assertTrue(hasNode(r, "temp.get"));
    }

    /** 变量名以文本形式出现在行内 molang 常量 → 不内联。 */
    @Test
    void textualReferenceBlocksInline() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("s", "exec.set_temp", opts("name", "temp.t0")),
                node("g", "temp.get", opts("name", "temp.t0")),
                node("r", "exec.return"),
                new NodeInstance("r2", "exec.return", 0, 0, Map.of(),
                        Map.of("value", new JsonPrimitive("temp.t0 * 2")))));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "exec_out", "r", "exec_in"),
                wire("g", "out", "r", "value")));

        VariableInliner.Result r = VariableInliner.inline(nodes, wires);

        assertTrue(hasNode(r, "exec.set_temp"));
        assertTrue(hasNode(r, "temp.get"));
    }

    /** set 在循环体外、读取在 body 内 → 不内联（跨边界）。 */
    @Test
    void loopBoundaryBlocksInline() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("s", "exec.set_temp", opts("name", "temp.t0")),
                node("g", "temp.get", opts("name", "temp.t0")),
                node("loop", "exec.loop"),
                node("bd", "exec.return")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "exec_out", "loop", "exec_in"),
                wire("bd", "exec_out", "loop", "body"),
                wire("g", "out", "bd", "value")));

        VariableInliner.Result r = VariableInliner.inline(nodes, wires);

        assertTrue(hasNode(r, "exec.set_temp"));
        assertTrue(hasNode(r, "temp.get"));
    }

    /** 写入值未连线 → 内联为消费端口的默认值常量（0）。 */
    @Test
    void unwiredValueInlinesDefaultConstant() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("s", "exec.set_temp", opts("name", "temp.t0")),
                node("g", "temp.get", opts("name", "temp.t0")),
                node("r", "exec.return")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "exec_out", "r", "exec_in"),
                wire("g", "out", "r", "value")));

        VariableInliner.Result r = VariableInliner.inline(nodes, wires);

        assertFalse(hasNode(r, "exec.set_temp"));
        NodeInstance ret = r.nodes().stream().filter(n -> n.uid().equals("r")).findFirst().orElseThrow();
        assertEquals(new JsonPrimitive(0), ret.constants().get("value"));
        assertFalse(hasWire(r, "s", "exec_out", "r", "exec_in"));
    }

    /** variable 被两个 set_var 写入 → 不内联。 */
    @Test
    void multiWriteNotInlined() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("v", "variable", opts("name", "hp")),
                node("s1", "exec.set_var"),
                node("s2", "exec.set_var"),
                node("r", "exec.return")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("v", "out", "s1", "target"),
                wire("v", "out", "s2", "target"),
                wire("s1", "exec_out", "s2", "exec_in"),
                wire("s2", "exec_out", "r", "exec_in"),
                wire("v", "out", "r", "value")));

        VariableInliner.Result r = VariableInliner.inline(nodes, wires);

        assertTrue(hasNode(r, "variable"));
        assertTrue(hasNode(r, "exec.set_var"));
    }

    /** 端到端：importer 旗标开/关的产物差异（initialize 脚本 temp 单写单读）。 */
    @Test
    void importerFlagControlsInlining() {
        String json = """
                {"minecraft:client_entity": {"description": {
                    "identifier": "test:inline",
                    "scripts": {"initialize": ["temp.t0 = 5; variable.hp = temp.t0;"]}
                }}}
                """;
        ImportResult off = JsonGraphImporters.importClientEntity(parse(json));
        List<NodeInstance> offNodes = off.library().mainGraph().nodes();
        assertTrue(offNodes.stream().anyMatch(n -> n.type().equals("exec.set_temp")));
        assertTrue(offNodes.stream().anyMatch(n -> n.type().equals("temp.get")));

        ImportResult on = JsonGraphImporters.importClientEntity(parse(json),
                id -> java.util.Optional.empty(), true);
        List<NodeInstance> onNodes = on.library().mainGraph().nodes();
        assertFalse(onNodes.stream().anyMatch(n -> n.type().equals("exec.set_temp")));
        assertFalse(onNodes.stream().anyMatch(n -> n.type().equals("temp.get")));
        // variable.hp 只写不读且内联后值为常量 5 → 初始化折叠为声明默认值（nodegraph-init-default-fold）
        assertFalse(onNodes.stream().anyMatch(n -> n.type().equals("exec.set_var")));
        var hpDecl = on.library().mainGraph().variables().stream()
                .filter(d -> d.name().equals("hp")).findFirst().orElseThrow();
        assertEquals(new JsonPrimitive(5), hpDecl.defaultValue().orElseThrow());
        assertTrue(DecompileTestSupport.hasCode(on.diagnostics(), VariableInliner.INLINED_VARIABLE));
        assertTrue(DecompileTestSupport.hasCode(on.diagnostics(), InitDefaultFolder.INIT_DEFAULT_FOLD));
    }
}
