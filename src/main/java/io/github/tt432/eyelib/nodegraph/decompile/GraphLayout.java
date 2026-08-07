package io.github.tt432.eyelib.nodegraph.decompile;

import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.StickyNote;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
/**
 * 分层自动布局（规格 nodegraph-workbench §W2）：<b>到汇最长路径定 x 层；层内先重心排序、
 * 再距离松弛定 y</b>。
 *
 * <p>坐标遵循编辑器既有约定（versions/1.20.1/run/config/eyelib/nodegraph/ng_smoke.json）：
 * 数据流方向 x 左→右（生产者左、消费者右，汇——装配根——在最右列），列距 300、最小行距 110。
 * 无连线的孤立节点（如声明表 ref.*）放在汇左一列。
 *
 * <p>y 分两个阶段（都是确定性纯函数）：
 * <ol>
 *   <li><b>重心排序</b>（Sugiyama 风格）：以「汇出发反向 DFS 先序」为种子，交替按消费者/生产者
 *       的平均 y 重排层内顺序——保证同簇节点在层内连续、不被无关节点隔开（纯 DFS 序的缺陷：
 *       深兄弟子树会把后访簇的成员挤到远离其消费者的位置）。</li>
 *   <li><b>距离松弛</b>：每轮把每个节点向「全部邻居的平均 y」拉拢（高斯-塞德尔迭代，
 *       层内顺序与最小行距用 clamp 保持）——让相连节点在距离上也贴近，而不是死守均布行。
 *       凸问题迭代必收敛；轮数按图规模给足并带早退。</li>
 * </ol>
 * 环只做防御（层按 0 计），环本身由 GraphValidator.CYCLE 报告。
 */
public final class GraphLayout {
    private GraphLayout() {
    }

    /** 列距（同 ng_smoke.json 的 300）。 */
    public static final float X_SPACING = 300f;
    /** 最小行距（节点不重叠的垂直间距下限）。 */
    public static final float Y_SPACING = 110f;

    /** 重心排序扫描上限（实践中远小于此即收敛）。 */
    private static final int ORDER_SWEEPS = 24;
    /** 松弛早退阈值：单轮最大位移低于此值即视为收敛。 */
    private static final double RELAX_EPSILON = 0.5;

    /**
     * 分层布局：返回坐标重排后的节点列表（其余字段直通，顺序与输入一致）。
     *
     * <p>层定义：汇（无出线的节点）为 0 层；其余节点 = 1 + 消费者层最大值（到汇最长路径）。
     * x = (maxLayer − layer) × {@link #X_SPACING}（汇在最右）；y 见类级文档两阶段。
     */
    public static List<NodeInstance> layout(List<NodeInstance> nodes, List<Wire> wires) {
        Map<String, List<String>> consumers = new HashMap<>();
        Map<String, List<String>> producers = new HashMap<>();
        Map<String, List<Wire>> producerWires = new HashMap<>();
        Set<String> wiredNodes = new HashSet<>();
        for (Wire w : wires) {
            consumers.computeIfAbsent(w.from().node(), k -> new ArrayList<>()).add(w.to().node());
            producers.computeIfAbsent(w.to().node(), k -> new ArrayList<>()).add(w.from().node());
            producerWires.computeIfAbsent(w.to().node(), k -> new ArrayList<>()).add(w);
            wiredNodes.add(w.from().node());
            wiredNodes.add(w.to().node());
        }
        consumers.values().forEach(list -> list.sort(Comparator.naturalOrder()));
        producers.values().forEach(list -> list.sort(Comparator.naturalOrder()));
        producerWires.values().forEach(list -> list.sort(
                Comparator.comparing((Wire w) -> w.from().node()).thenComparing(w -> w.from().port())));

        // x 层：到汇最长路径（记忆化 DFS；环防御 → 0）
        Map<String, Integer> layers = new HashMap<>();
        int maxLayerValue = 0;
        for (NodeInstance node : nodes) {
            if (wiredNodes.contains(node.uid())) {
                int layer = layerOf(node.uid(), consumers, layers, new HashSet<>());
                maxLayerValue = Math.max(maxLayerValue, layer);
            }
        }
        final int maxLayer = maxLayerValue;
        // 孤立节点（无任何连线，如声明表 ref.*）：汇左一列（无汇时同列）
        int isolatedLayer = maxLayer > 0 ? 1 : 0;

        // y 种子序：汇（uid 字典序）出发，沿连线反向 DFS 的先序序号
        Map<String, Integer> order = new HashMap<>();
        List<String> sinks = new ArrayList<>();
        for (NodeInstance node : nodes) {
            if (!consumers.containsKey(node.uid())) {
                sinks.add(node.uid());
            }
        }
        sinks.sort(Comparator.naturalOrder());
        Set<String> visited = new HashSet<>();
        int[] seq = {0};
        for (String sink : sinks) {
            dfsOrder(sink, producerWires, visited, order, seq);
        }

        // 同层分组（层号升序 = 汇→源，确定性遍历），按 DFS 种子序排定初始顺序
        Map<Integer, List<NodeInstance>> byLayer = new TreeMap<>();
        for (NodeInstance node : nodes) {
            int layer = wiredNodes.contains(node.uid()) ? layers.get(node.uid()) : isolatedLayer;
            byLayer.computeIfAbsent(layer, k -> new ArrayList<>()).add(node);
        }
        byLayer.values().forEach(group -> group.sort(Comparator
                .comparingInt((NodeInstance n) -> order.getOrDefault(n.uid(), Integer.MAX_VALUE))
                .thenComparing(NodeInstance::uid)));

        // 初始均布 y
        Map<String, Double> ys = new HashMap<>();
        byLayer.values().forEach(group -> {
            for (int i = 0; i < group.size(); i++) {
                ys.put(group.get(i).uid(), (double) (i * Y_SPACING));
            }
        });

        orderByBarycenter(byLayer, consumers, producers, ys);
        relaxDistances(byLayer, consumers, producers, ys, nodes.size());

        List<NodeInstance> out = new ArrayList<>(nodes.size());
        for (NodeInstance n : nodes) {
            int layer = wiredNodes.contains(n.uid()) ? layers.get(n.uid()) : isolatedLayer;
            float x = (maxLayer - layer) * X_SPACING;
            float y = ys.getOrDefault(n.uid(), 0.0).floatValue();
            out.add(new NodeInstance(n.uid(), n.type(), x, y, n.options(), n.constants()));
        }
        return out;
    }

    /** 便签定位：图最右列右侧一列，自上而下堆叠（其余字段直通）。 */
    public static List<StickyNote> placeStickyNotes(List<StickyNote> notes, List<NodeInstance> laidOutNodes) {
        float maxX = 0;
        for (NodeInstance n : laidOutNodes) {
            maxX = Math.max(maxX, n.x());
        }
        List<StickyNote> out = new ArrayList<>(notes.size());
        for (int i = 0; i < notes.size(); i++) {
            StickyNote n = notes.get(i);
            out.add(new StickyNote(n.uid(), n.text(), maxX + X_SPACING, i * Y_SPACING,
                    n.width(), n.height(), n.color()));
        }
        return out;
    }

    /**
     * 重心排序：交替「按消费者平均 y」（汇→源方向逐层）与「按生产者平均 y」（源→汇方向逐层）
     * 重排层内顺序并重置均布 y。无该方向邻居的节点以自身 y 为重心（位置稳定，沉底）。
     * 次序比较带旧 y 与 uid 双重决胜，全程确定性。
     */
    private static void orderByBarycenter(Map<Integer, List<NodeInstance>> byLayer,
                                          Map<String, List<String>> consumers,
                                          Map<String, List<String>> producers,
                                          Map<String, Double> ys) {
        List<List<NodeInstance>> groups = new ArrayList<>(byLayer.values()); // TreeMap 按层号升序
        for (int sweep = 0; sweep < ORDER_SWEEPS; sweep++) {
            boolean changed = false;
            // 按消费者重心：源侧层 → 汇侧层（层号降序）
            for (int i = groups.size() - 1; i >= 0; i--) {
                changed |= sortLayerByBarycenter(groups.get(i), consumers, ys);
            }
            // 按生产者重心：汇侧层 → 源侧层（层号升序）
            for (List<NodeInstance> group : groups) {
                changed |= sortLayerByBarycenter(group, producers, ys);
            }
            if (!changed) {
                return;
            }
        }
    }

    /** 单趟层内重心排序；顺序有变化返回 true。排序后重置为均布行 y。 */
    private static boolean sortLayerByBarycenter(List<NodeInstance> group,
                                                 Map<String, List<String>> neighbors,
                                                 Map<String, Double> ys) {
        Map<String, Double> barycenters = new HashMap<>();
        for (NodeInstance n : group) {
            barycenters.put(n.uid(), meanNeighborY(n.uid(), neighbors, ys));
        }
        List<NodeInstance> sorted = new ArrayList<>(group);
        sorted.sort(Comparator
                .comparingDouble((NodeInstance n) -> barycenters.getOrDefault(n.uid(), 0.0))
                .thenComparingDouble(n -> yOf(ys, n.uid()))
                .thenComparing(NodeInstance::uid));
        boolean changed = false;
        for (int i = 0; i < sorted.size(); i++) {
            NodeInstance n = sorted.get(i);
            if (group.get(i) != n) {
                changed = true;
            }
            group.set(i, n);
            ys.put(n.uid(), (double) (i * Y_SPACING));
        }
        return changed;
    }

    /**
     * 距离松弛：高斯-塞德尔迭代，每轮每个节点向「全部邻居（生产者+消费者）的平均 y」
     * 拉拢，clamp 保持层内顺序与最小行距。孤立节点无邻居不参与（留在均布位）。
     */
    private static void relaxDistances(Map<Integer, List<NodeInstance>> byLayer,
                                       Map<String, List<String>> consumers,
                                       Map<String, List<String>> producers,
                                       Map<String, Double> ys, int nodeCount) {
        int maxIterations = 2 * nodeCount + 16;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            double maxDelta = 0;
            for (List<NodeInstance> group : byLayer.values()) {
                double prevY = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < group.size(); i++) {
                    NodeInstance n = group.get(i);
                    double current = yOf(ys, n.uid());
                    double nextY = i + 1 < group.size()
                            ? yOf(ys, group.get(i + 1).uid()) : Double.POSITIVE_INFINITY;
                    double desired = meanAllNeighborsY(n.uid(), consumers, producers, ys);
                    if (Double.isNaN(desired)) {
                        prevY = current; // 孤立节点：不动
                        continue;
                    }
                    double clamped = Math.min(Math.max(desired, prevY + Y_SPACING), nextY - Y_SPACING);
                    if (clamped != current) {
                        ys.put(n.uid(), clamped);
                        maxDelta = Math.max(maxDelta, Math.abs(clamped - current));
                    }
                    prevY = clamped;
                }
            }
            if (maxDelta < RELAX_EPSILON) {
                return;
            }
        }
    }

    /** 指定方向邻居的平均 y；无邻居返回自身 y（排序语义：位置稳定）。 */
    private static double meanNeighborY(String uid, Map<String, List<String>> neighbors,
                                        Map<String, Double> ys) {
        List<String> list = neighbors.get(uid);
        if (list == null || list.isEmpty()) {
            return yOf(ys, uid);
        }
        double sum = 0;
        for (String other : list) {
            sum += yOf(ys, other);
        }
        return sum / list.size();
    }

    /** 全部邻居（双向）的平均 y；无任何邻居返回 NaN（松弛语义：跳过）。 */
    private static double meanAllNeighborsY(String uid, Map<String, List<String>> consumers,
                                            Map<String, List<String>> producers, Map<String, Double> ys) {
        double sum = 0;
        int count = 0;
        for (String other : consumers.getOrDefault(uid, List.of())) {
            sum += yOf(ys, other);
            count++;
        }
        for (String other : producers.getOrDefault(uid, List.of())) {
            sum += yOf(ys, other);
            count++;
        }
        return count == 0 ? Double.NaN : sum / count;
    }

    /** 读取节点 y（悬空线端点按 0 计—— dangling 由 GraphValidator 报告，布局保持全函数）。 */

    private static double yOf(Map<String, Double> ys, String uid) {
        return ys.getOrDefault(uid, 0.0);
    }

    private static int layerOf(String uid, Map<String, List<String>> consumers,
                               Map<String, Integer> cache, Set<String> visiting) {
        Integer cached = cache.get(uid);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(uid)) {
            return 0; // 环防御：不挂死，环由 GraphValidator.CYCLE 报告
        }
        int layer = 0;
        for (String consumer : consumers.getOrDefault(uid, List.of())) {
            layer = Math.max(layer, layerOf(consumer, consumers, cache, visiting) + 1);
        }
        visiting.remove(uid);
        cache.put(uid, layer);
        return layer;
    }

    private static void dfsOrder(String uid, Map<String, List<Wire>> producers,
                                 Set<String> visited, Map<String, Integer> order, int[] seq) {
        if (!visited.add(uid)) {
            return;
        }
        order.put(uid, seq[0]++);
        for (Wire w : producers.getOrDefault(uid, List.of())) {
            dfsOrder(w.from().node(), producers, visited, order, seq);
        }
    }
}
