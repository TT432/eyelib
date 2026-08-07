package io.github.tt432.eyelib.nodegraph.decompile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.StickyNote;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link GraphLayout} 单测：到汇最长路径定 x 层（汇最右）、重心排序 + 距离松弛定 y
 * （相连节点贴近）、孤立节点位置、便签定位、确定性。
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
        // 孤立节点无邻居，不参与松弛：留在均布行
        assertEquals(GraphLayout.Y_SPACING, find(laid, "z").y());
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

    @Test
    void independentChainsStayVerticallyAligned() {
        // 两条互不相连的链：每条链的相邻节点垂直距离不超过一个行距（理想 = 0）
        List<NodeInstance> laid = GraphLayout.layout(
                List.of(node("x1"), node("m1"), node("s1"), node("x2"), node("m2"), node("s2")),
                List.of(wire("x1", "m1"), wire("m1", "s1"), wire("x2", "m2"), wire("m2", "s2")));

        assertClose(find(laid, "x1"), find(laid, "m1"));
        assertClose(find(laid, "m1"), find(laid, "s1"));
        assertClose(find(laid, "x2"), find(laid, "m2"));
        assertClose(find(laid, "m2"), find(laid, "s2"));
        // 两簇不交错：链 1 整体在上
        assertTrue(find(laid, "m1").y() < find(laid, "m2").y());
        assertTrue(find(laid, "x1").y() < find(laid, "x2").y());
    }

    @Test
    void distantClusterMemberPulledNearItsConsumer() {
        // c2 有 5 个生产者 b1..b5：旧「DFS 序 × 均布行」把 b5 放到 y=550，距其消费者
        // c2（y=110）440px。重心排序 + 距离松弛后该距离必须严格小于旧值。
        List<NodeInstance> laid = GraphLayout.layout(
                List.of(node("s"), node("c1"), node("c2"), node("a"),
                        node("b1"), node("b2"), node("b3"), node("b4"), node("b5")),
                List.of(wire("c1", "s"), wire("c2", "s"), wire("a", "c1"),
                        wire("b1", "c2"), wire("b2", "c2"), wire("b3", "c2"),
                        wire("b4", "c2"), wire("b5", "c2")));

        float oldUniformDfsDistance = 440f;
        float dist = Math.abs(find(laid, "b5").y() - find(laid, "c2").y());
        assertTrue(dist < oldUniformDfsDistance, "b5 与其消费者 c2 的距离 " + dist + " 应小于旧布局 440");
        // 兄弟消费者都贴近汇
        assertClose(find(laid, "c1"), find(laid, "s"));
        assertClose(find(laid, "c2"), find(laid, "s"));
    }

    /** 相连节点垂直距离不超过一个行距。 */
    private static void assertClose(NodeInstance producer, NodeInstance consumer) {
        assertTrue(Math.abs(producer.y() - consumer.y()) <= GraphLayout.Y_SPACING,
                () -> producer.uid() + " 与 " + consumer.uid() + " 垂直距离 "
                        + Math.abs(producer.y() - consumer.y()) + " 超过行距 " + GraphLayout.Y_SPACING);
    }
}
