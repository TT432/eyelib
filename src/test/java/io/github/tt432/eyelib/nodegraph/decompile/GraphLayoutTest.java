package io.github.tt432.eyelib.nodegraph.decompile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.StickyNote;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link GraphLayout} 单测：到汇最长路径定 x 层（汇最右）、DFS 序定 y、
 * 孤立节点位置、便签定位、确定性。
 */
class GraphLayoutTest {

    private static NodeInstance node(String uid) {
        return NodeInstance.of(uid, "const.number", 0, 0);
    }

    private static Wire wire(String from, String to) {
        return new Wire(new PortRef(from, "out"), new PortRef(to, "in"));
    }

    private static NodeInstance find(List<NodeInstance> nodes, String uid) {
        return nodes.stream().filter(n -> n.uid().equals(uid)).findFirst().orElseThrow();
    }

    @Test
    void linearChainLayers() {
        // a → b → c（c 为汇）：x 左→右为数据流方向，列距 300
        List<NodeInstance> laid = GraphLayout.layout(
                List.of(node("a"), node("b"), node("c")),
                List.of(wire("a", "b"), wire("b", "c")));

        assertEquals(0f, find(laid, "a").x());
        assertEquals(GraphLayout.X_SPACING, find(laid, "b").x());
        assertEquals(2 * GraphLayout.X_SPACING, find(laid, "c").x());
    }

    @Test
    void sameLayerNodesStackByDfsOrder() {
        // 两个生产者同层：y 按汇出发反向 DFS 序堆叠，行距 110
        List<NodeInstance> laid = GraphLayout.layout(
                List.of(node("p1"), node("p2"), node("c")),
                List.of(wire("p1", "c"), wire("p2", "c")));

        assertEquals(0f, find(laid, "p1").x());
        assertEquals(0f, find(laid, "p2").x());
        assertEquals(GraphLayout.X_SPACING, find(laid, "c").x());
        assertEquals(0f, find(laid, "p1").y());
        assertEquals(GraphLayout.Y_SPACING, find(laid, "p2").y());
    }

    @Test
    void isolatedNodeSitsLeftOfSink() {
        // 孤立节点（无连线，如声明表 ref.*）：汇左一列
        List<NodeInstance> laid = GraphLayout.layout(
                List.of(node("a"), node("c"), node("z")),
                List.of(wire("a", "c")));

        assertEquals(GraphLayout.X_SPACING, find(laid, "c").x());
        assertEquals(0f, find(laid, "z").x());
    }

    @Test
    void longestPathWins() {
        // d → a → c 且 d → c：a 的层按到汇最长路径（d→a→c = 2，d→c = 1）
        List<NodeInstance> laid = GraphLayout.layout(
                List.of(node("a"), node("c"), node("d")),
                List.of(wire("d", "a"), wire("a", "c"), wire("d", "c")));

        assertEquals(2 * GraphLayout.X_SPACING, find(laid, "c").x());
        assertEquals(GraphLayout.X_SPACING, find(laid, "a").x());
        assertEquals(0f, find(laid, "d").x());
    }

    @Test
    void cycleDoesNotHang() {
        List<NodeInstance> laid = GraphLayout.layout(
                List.of(node("a"), node("b")),
                List.of(wire("a", "b"), wire("b", "a")));
        assertEquals(2, laid.size());
    }

    @Test
    void deterministic() {
        List<NodeInstance> nodes = List.of(node("n1"), node("n2"), node("n3"), node("n4"));
        List<Wire> wires = List.of(wire("n1", "n3"), wire("n2", "n3"), wire("n3", "n4"));
        assertEquals(GraphLayout.layout(nodes, wires), GraphLayout.layout(nodes, wires));
    }

    @Test
    void stickyNotesPlacedRightOfGraph() {
        List<NodeInstance> laid = GraphLayout.layout(
                List.of(node("a"), node("c")), List.of(wire("a", "c")));
        List<StickyNote> notes = GraphLayout.placeStickyNotes(List.of(
                new StickyNote("s0", "one", 0, 0, 200, 100, "#FFFF88"),
                new StickyNote("s1", "two", 0, 0, 200, 100, "#FFFF88")), laid);

        assertEquals(GraphLayout.X_SPACING + GraphLayout.X_SPACING, notes.get(0).x());
        assertEquals(0f, notes.get(0).y());
        assertEquals(GraphLayout.Y_SPACING, notes.get(1).y());
        assertEquals("one", notes.get(0).text());
    }

    @Test
    void emptyGraph() {
        assertEquals(List.of(), GraphLayout.layout(List.of(), List.of()));
        assertEquals(List.of(), GraphLayout.placeStickyNotes(List.of(), List.of()));
    }

    @Test
    void unrelatedFieldsPassThrough() {
        NodeInstance original = new NodeInstance("a", "op.binary", 0, 0,
                Map.of(), Map.of());
        List<NodeInstance> laid = GraphLayout.layout(List.of(original), List.of());
        assertEquals(original.type(), laid.get(0).type());
        assertEquals(original.options(), laid.get(0).options());
    }
}
