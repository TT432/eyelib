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

    @Test
    void fanInClusterCentersOnItsHub() {
        // 7 个单连线生产者（同层）扇入同一 hub：逐节点贪心下扇形从 hub 单向堆叠
        // （最远 6×110）；压紧段刚性平移后扇形以 hub 为中心（最远 ≤ 3×110+容差）。
        List<NodeInstance> nodes = new java.util.ArrayList<>();
        nodes.add(node("hub"));
        List<Wire> wires = new java.util.ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            nodes.add(node("leaf" + i));
            wires.add(wire("leaf" + i, "hub"));
        }
        List<NodeInstance> laid = GraphLayout.layout(nodes, wires);

        float hubY = find(laid, "hub").y();
        java.util.List<Float> leafYs = new java.util.ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            leafYs.add(find(laid, "leaf" + i).y());
        }
        float maxDist = leafYs.stream().map(y -> Math.abs(y - hubY)).max(Float::compare).orElseThrow();
        assertTrue(maxDist <= 3 * GraphLayout.Y_SPACING + 1f,
                "扇形边缘成员距 hub " + maxDist + " 超过居中上限 " + (3 * GraphLayout.Y_SPACING + 1f));
        // 层内最小行距保持（不重叠）
        leafYs.sort(Float::compare);
        for (int i = 1; i < leafYs.size(); i++) {
            assertTrue(leafYs.get(i) - leafYs.get(i - 1) >= GraphLayout.Y_SPACING - 0.01f,
                    "相邻扇形成员间距 " + (leafYs.get(i) - leafYs.get(i - 1)) + " 小于行距");
        }
    }

    /** 相连节点垂直距离不超过一个行距。 */
    private static void assertClose(NodeInstance producer, NodeInstance consumer) {
        assertTrue(Math.abs(producer.y() - consumer.y()) <= GraphLayout.Y_SPACING,
                () -> producer.uid() + " 与 " + consumer.uid() + " 垂直距离 "
                        + Math.abs(producer.y() - consumer.y()) + " 超过行距 " + GraphLayout.Y_SPACING);
    }

    @Test
    void giantFanSplitsIntoMultipleColumns() {
        // 40 叶纯叶扇超过单列上限（16）：拆成多列后每个子扇以 hub 居中，
        // 最大边距从 (40−1)/2×110 压到 (16−1)/2×110
        List<NodeInstance> nodes = new java.util.ArrayList<>();
        nodes.add(node("hub"));
        List<Wire> wires = new java.util.ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            nodes.add(node("leaf" + i));
            wires.add(wire("leaf" + i, "hub"));
        }
        List<NodeInstance> laid = GraphLayout.layout(nodes, wires);

        float hubY = find(laid, "hub").y();
        java.util.Set<Float> leafColumns = new java.util.HashSet<>();
        float maxDist = 0;
        for (int i = 1; i <= 40; i++) {
            NodeInstance leaf = find(laid, "leaf" + i);
            maxDist = Math.max(maxDist, Math.abs(leaf.y() - hubY));
            leafColumns.add(leaf.x());
        }
        assertEquals(3, leafColumns.size(), "40 叶应拆成 3 列（16+16+8），实际 " + leafColumns.size());
        assertTrue(maxDist <= 15 / 2.0 * GraphLayout.Y_SPACING + 1f,
                "分列后扇形边缘成员距 hub " + maxDist + " 超过上限 "
                        + (15 / 2.0 * GraphLayout.Y_SPACING + 1f));

        // 每列内部最小行距保持（不重叠）
        for (float columnX : leafColumns) {
            java.util.List<Float> columnYs = new java.util.ArrayList<>();
            for (int i = 1; i <= 40; i++) {
                NodeInstance leaf = find(laid, "leaf" + i);
                if (leaf.x() == columnX) {
                    columnYs.add(leaf.y());
                }
            }
            columnYs.sort(Float::compare);
            for (int i = 1; i < columnYs.size(); i++) {
                assertTrue(columnYs.get(i) - columnYs.get(i - 1) >= GraphLayout.Y_SPACING - 0.01f,
                        "同列相邻扇成员间距 " + (columnYs.get(i) - columnYs.get(i - 1)) + " 小于行距");
            }
        }
    }

    @Test
    void chainFedGiantFanSplitsWithoutBreakingEdgeDirections() {
        // 成员自带生产者的巨扇（度数>1，如带表达式链的 rc.root→entity.root）：
        // 唯一消费者同为 hub 即满足分列条件；分列不强行移动成员（链锚定），
        // 但必须保持边方向不倒退、列数正确
        List<NodeInstance> nodes = new java.util.ArrayList<>();
        nodes.add(node("hub"));
        List<Wire> wires = new java.util.ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            nodes.add(node("leaf" + i));
            nodes.add(node("chain" + i));
            wires.add(wire("leaf" + i, "hub"));
            wires.add(wire("chain" + i, "leaf" + i)); // 每个叶带一个上游
        }
        List<NodeInstance> laid = GraphLayout.layout(nodes, wires);

        java.util.Set<Float> leafColumns = new java.util.HashSet<>();
        for (int i = 1; i <= 30; i++) {
            NodeInstance leaf = find(laid, "leaf" + i);
            leafColumns.add(leaf.x());
            assertTrue(find(laid, "chain" + i).x() < leaf.x(), "chain" + i + " 应在 leaf" + i + " 左侧");
            assertTrue(leaf.x() < find(laid, "hub").x(), "leaf" + i + " 应在 hub 左侧");
        }
        assertEquals(2, leafColumns.size(), "30 叶应拆成 2 列（16+14），实际 " + leafColumns.size());
    }

    @Test
    void hubMovesToFanSpanCenterWhenClearlyBetter() {
        // 20 个纯叶扇成员被链锚定在 y=0..2090（ hub 的 L1 最优点偏向密集侧）；
        // hub 独占一列可自由移动。极小极大定位把 hub 移到扇跨中心，
        // 扇最大边 = 跨度一半；实测判定保证只在明显改善时移动
        List<NodeInstance> nodes = new java.util.ArrayList<>();
        nodes.add(node("hub"));
        List<Wire> wires = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            nodes.add(node("leaf" + i));
            nodes.add(node("anchor" + i));
            // anchor 全挂在远处的一个源上，把 leaf 链锚到远离 hub 的位置
            wires.add(wire("leaf" + i, "hub"));
            wires.add(wire("anchor" + i, "leaf" + i));
        }
        nodes.add(node("farSource"));
        for (int i = 0; i < 20; i++) {
            wires.add(wire("farSource", "anchor" + i));
        }
        List<NodeInstance> laid = GraphLayout.layout(nodes, wires);

        float hubY = find(laid, "hub").y();
        float maxFanEdge = 0;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        for (int i = 0; i < 20; i++) {
            float y = find(laid, "leaf" + i).y();
            maxFanEdge = Math.max(maxFanEdge, Math.abs(y - hubY));
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        float halfSpan = (maxY - minY) / 2;
        assertTrue(maxFanEdge <= halfSpan + GraphLayout.Y_SPACING,
                "hub 应在扇跨中心附近：maxFanEdge=" + maxFanEdge + " halfSpan=" + halfSpan);
    }
}
