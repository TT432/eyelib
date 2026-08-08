package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.Wire;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.countCode;
import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.opts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ConstNodeInliner 单测（规格 nodegraph-import-const-inline）。
 */
class ConstNodeInlinerTest {

    private static NodeInstance node(String uid, String type) {
        return new NodeInstance(uid, type, 0, 0, Map.of(), Map.of());
    }

    private static NodeInstance node(String uid, String type, Map<String, JsonElement> options) {
        return new NodeInstance(uid, type, 0, 0, options, Map.of());
    }

    private static Wire wire(String fromNode, String fromPort, String toNode, String toPort) {
        return new Wire(new PortRef(fromNode, fromPort), new PortRef(toNode, toPort));
    }

    @Test
    void singleUseConstIntInlinesIntoPortConstants() {
        // const.int(5) → animate.entry.weight（FLOAT 带默认 1，可内联）
        List<NodeInstance> nodes = new java.util.ArrayList<>(List.of(
                node("c", "const.int", opts("value", new JsonPrimitive(5))),
                node("ae", "animate.entry")));
        List<Wire> wires = new java.util.ArrayList<>(List.of(wire("c", "out", "ae", "weight")));

        ConstNodeInliner.Result r = ConstNodeInliner.inline(nodes, wires);

        assertTrue(r.nodes().stream().noneMatch(n -> n.uid().equals("c")), "const 节点应删除");
        assertTrue(r.wires().isEmpty(), "连线应删除");
        NodeInstance ae = r.nodes().stream().filter(n -> n.uid().equals("ae")).findFirst().orElseThrow();
        assertEquals(5, ae.constants().get("weight").getAsInt(), "常量应落到 weight 行内值");
        assertEquals(1, countCode(r.diagnostics(), ConstNodeInliner.CONST_INLINE));
    }

    @Test
    void multiUseConstKept() {
        List<NodeInstance> nodes = new java.util.ArrayList<>(List.of(
                node("c", "const.int", opts("value", new JsonPrimitive(5))),
                node("a", "animate.entry"),
                node("b", "animate.entry")));
        List<Wire> wires = new java.util.ArrayList<>(List.of(
                wire("c", "out", "a", "weight"),
                wire("c", "out", "b", "weight")));

        ConstNodeInliner.Result r = ConstNodeInliner.inline(nodes, wires);

        assertTrue(r.nodes().stream().anyMatch(n -> n.uid().equals("c")), "共享常量应保留");
        assertEquals(2, r.wires().size());
        assertTrue(r.diagnostics().isEmpty());
    }

    @Test
    void constStringKeepsStringTypeOnAnyPort() {
        // const.string("5") → exec.set_var.value（ANY 带默认 0）：必须保持字符串，不能被智能解析成 int
        List<NodeInstance> nodes = new java.util.ArrayList<>(List.of(
                node("c", "const.string", opts("value", new JsonPrimitive("5"))),
                node("s", "exec.set_var")));
        List<Wire> wires = new java.util.ArrayList<>(List.of(wire("c", "out", "s", "value")));

        ConstNodeInliner.Result r = ConstNodeInliner.inline(nodes, wires);

        NodeInstance s = r.nodes().stream().filter(n -> n.uid().equals("s")).findFirst().orElseThrow();
        JsonElement v = s.constants().get("value");
        assertTrue(v.isJsonPrimitive() && v.getAsJsonPrimitive().isString(),
                "const.string 内联后必须保持字符串形态");
        assertEquals("5", v.getAsString());
    }

    @Test
    void portWithoutDefaultNotInlined() {
        // entity.root.scale_x 无默认值 → 无行内编辑器，内联会让值隐身
        List<NodeInstance> nodes = new java.util.ArrayList<>(List.of(
                node("c", "const.number", opts("value", new JsonPrimitive(1.5))),
                node("root", "entity.root")));
        List<Wire> wires = new java.util.ArrayList<>(List.of(wire("c", "out", "root", "scale_x")));

        ConstNodeInliner.Result r = ConstNodeInliner.inline(nodes, wires);

        assertTrue(r.nodes().stream().anyMatch(n -> n.uid().equals("c")), "无默认值端口不应内联");
        assertEquals(1, r.wires().size());
    }

    @Test
    void constColorNotInlined() {
        List<NodeInstance> nodes = new java.util.ArrayList<>(List.of(
                node("c", "const.color", opts("value", new JsonPrimitive("#FFFF0000"))),
                node("rc", "rc.root")));
        List<Wire> wires = new java.util.ArrayList<>(List.of(wire("c", "out", "rc", "color")));

        ConstNodeInliner.Result r = ConstNodeInliner.inline(nodes, wires);

        assertTrue(r.nodes().stream().anyMatch(n -> n.uid().equals("c")), "const.color 不动");
        assertEquals(1, r.wires().size());
    }

    @Test
    void orphanConstUntouched() {
        List<NodeInstance> nodes = new java.util.ArrayList<>(List.of(
                node("c", "const.int", opts("value", new JsonPrimitive(5)))));

        ConstNodeInliner.Result r = ConstNodeInliner.inline(nodes, List.of());

        assertEquals(1, r.nodes().size());
        assertTrue(r.diagnostics().isEmpty());
        assertFalse(countCode(r.diagnostics(), ConstNodeInliner.CONST_INLINE) > 0);
    }
}
