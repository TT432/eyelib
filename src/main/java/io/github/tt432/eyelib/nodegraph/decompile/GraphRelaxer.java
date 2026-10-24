package io.github.tt432.eyelib.nodegraph.decompile;

import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 层内距离调整：高斯-塞德尔松弛、纯叶扇刚体居中、列内欲望重排，以及布局终态坐标
 * （y 归一化、芯片贴靠与重叠消解）与全部布局阶段共享的 y/高度/中位数读取原语。
 *
 * <p>无实例状态：全部方法为纯函数（只读写传入的 {@code ys}/{@code heights} 映射与列分组），
 * 由 {@link GraphLayout} 的装配流程调用。坐标常量沿用 {@link GraphLayout#X_SPACING}、
 * {@link GraphLayout#Y_SPACING}、{@link GraphLayout#NODE_MARGIN}。
 */
final class GraphRelaxer {
    private GraphRelaxer() {
    }

    /** 松弛早退阈值：单轮最大位移低于此值即视为收敛。 */
    private static final double RELAX_EPSILON = 0.5;

    /** 芯片与锚点的水平净距。 */
    static final float CHIP_GAP_X = 8f;
    /** 同锚点同侧芯片的堆叠净距。 */
    private static final float CHIP_STACK_GAP = 6f;
    /** 芯片重叠消解趟数上限。 */
    private static final int CHIP_SEPARATION_SWEEPS = 60;

    /**
     * 芯片锚定放置：同（锚点, 侧）的芯片按 uid 序堆叠、行向锚点中心居中，
     * x = 锚点左/右缘 ± {@link #CHIP_GAP_X}（凸入列间隙，列距 300 − 节点宽 ~190 的
     * 间隙 ≈ 110，芯片 120 宽会凸入邻列 ≤ 70px——邻列同行为空，冲突由最后的
     * 仅芯片移动消解兜底）。
     */
    static void placeSnappedChips(Map<String, String> snapAnchor, Map<String, Integer> snapSide,
                                  Map<String, NodeInstance> byUid, Map<String, Float> heights,
                                  Map<String, Float> xsFinal, Map<String, Double> ys,
                                  Map<String, Integer> slotOfUid) {
        // 按（锚点槽位, 侧）分组：同间隙的芯片共享一条行游标（按锚点行排序，首个贴锚点行，
        // 后续顺排——不同锚点的芯片在同一间隙不互叠）
        Map<String, List<String>> groups = new TreeMap<>();
        for (Map.Entry<String, String> e : snapAnchor.entrySet()) {
            Integer slot = slotOfUid.get(e.getValue());
            if (slot == null) {
                continue;
            }
            groups.computeIfAbsent(slot + "" + snapSide.get(e.getKey()),
                    k -> new ArrayList<>()).add(e.getKey());
        }
        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            List<String> chips = entry.getValue();
            chips.sort(Comparator
                    .comparingDouble((String c) -> ys.getOrDefault(snapAnchor.get(c), 0.0))
                    .thenComparing(c -> snapAnchor.get(c))
                    .thenComparing(Comparator.naturalOrder()));
            double cursor = Double.NEGATIVE_INFINITY;
            int i = 0;
            while (i < chips.size()) {
                // 同锚点子块：块高已知，块顶 = max(游标, 锚点中心 − 块高/2)——
                // 芯片块居中于锚点（纯下排会让 19 芯片的 ref 尾部 dy 漂到 600）
                int j = i;
                while (j + 1 < chips.size()
                        && java.util.Objects.equals(snapAnchor.get(chips.get(j + 1)),
                                snapAnchor.get(chips.get(i)))) {
                    j++;
                }
                String anchor = snapAnchor.get(chips.get(i));
                NodeInstance anchorNode = byUid.get(anchor);
                if (anchorNode == null) {
                    i = j + 1;
                    continue;
                }
                int side = snapSide.getOrDefault(chips.get(i), -1);
                float anchorW = GraphLayout.estimateWidth(anchorNode);
                double anchorX = xsFinal.getOrDefault(anchor, 0f);
                double anchorCy = ys.getOrDefault(anchor, 0.0)
                        + heights.getOrDefault(anchor, GraphLayout.UNKNOWN_NODE_HEIGHT) / 2;
                double blockH = -CHIP_STACK_GAP;
                for (int k = i; k <= j; k++) {
                    blockH += heights.getOrDefault(chips.get(k), GraphLayout.UNKNOWN_NODE_HEIGHT)
                            + CHIP_STACK_GAP;
                }
                double top = Math.max(cursor, anchorCy - blockH / 2);
                for (int k = i; k <= j; k++) {
                    String chip = chips.get(k);
                    float chipW = GraphLayout.estimateWidth(java.util.Objects.requireNonNull(byUid.get(chip)));
                    float chipH = heights.getOrDefault(chip, GraphLayout.UNKNOWN_NODE_HEIGHT);
                    xsFinal.put(chip, (float) (side < 0
                            ? anchorX - chipW - CHIP_GAP_X
                            : anchorX + anchorW + CHIP_GAP_X));
                    ys.put(chip, top);
                    top += chipH + CHIP_STACK_GAP;
                }
                cursor = top;
                i = j + 1;
            }
        }
        // 仅芯片移动的重叠消解（uid 序、最小穿透轴、逐趟至无重叠）
        List<String> chipOrder = new ArrayList<>(snapAnchor.keySet());
        chipOrder.sort(Comparator.naturalOrder());
        for (int sweep = 0; sweep < CHIP_SEPARATION_SWEEPS; sweep++) {
            boolean moved = false;
            for (String chip : chipOrder) {
                if (!xsFinal.containsKey(chip) || !ys.containsKey(chip)) {
                    continue; // 锚点缺失未放置的芯片不参与消解
                }
                float cw = GraphLayout.estimateWidth(
                        java.util.Objects.requireNonNull(byUid.get(chip)));
                float ch = heights.getOrDefault(chip, GraphLayout.UNKNOWN_NODE_HEIGHT);
                double cx = xsFinal.getOrDefault(chip, 0f) + cw / 2;
                double cy = ys.getOrDefault(chip, 0.0) + ch / 2;
                for (NodeInstance other : byUid.values()) {
                    if (other.uid().equals(chip)) {
                        continue;
                    }
                    float ow = GraphLayout.estimateWidth(other);
                    float oh = heights.getOrDefault(other.uid(), GraphLayout.UNKNOWN_NODE_HEIGHT);
                    double ocx = xsFinal.getOrDefault(other.uid(), 0f) + ow / 2;
                    double ocy = ys.getOrDefault(other.uid(), 0.0) + oh / 2;
                    double ox = (cw + ow) / 2 - Math.abs(cx - ocx);
                    double oy = (ch + oh) / 2 - Math.abs(cy - ocy);
                    if (ox <= 0 || oy <= 0) {
                        continue;
                    }
                    if (ox < oy) {
                        xsFinal.put(chip, (float) (xsFinal.getOrDefault(chip, 0f)
                                + (cx < ocx ? -1 : 1) * (ox + 0.5)));
                    } else {
                        ys.put(chip, ys.getOrDefault(chip, 0.0) + (cy < ocy ? -1 : 1) * (oy + 0.5));
                    }
                    cx = xsFinal.getOrDefault(chip, 0f) + cw / 2;
                    cy = ys.getOrDefault(chip, 0.0) + ch / 2;
                    moved = true;
                }
            }
            if (!moved) {
                return;
            }
        }
    }

    /**
     * 距离松弛：高斯-塞德尔迭代。每轮两层动作：① 逐节点向「全部邻居（生产者+消费者）
     * 的中位数 y」拉拢（L1 目标的单点最优；clamp 保持层内顺序与节点高度间距）；② 纯叶扇居中（{@link #centerLeafFans}）。
     * 孤立节点无邻居不参与①（留在均布位）。
     */
    static void relaxDistances(Map<GraphLayout.ColumnKey, List<NodeInstance>> byLayer,
                               Map<String, List<String>> consumers,
                               Map<String, List<String>> producers,
                               Map<String, Double> ys, Map<String, String> soleHub,
                               Map<String, Float> heights, int nodeCount) {
        int maxIterations = 2 * nodeCount + 16;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            double maxDelta = 0;
            for (List<NodeInstance> group : byLayer.values()) {
                double prevBottom = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < group.size(); i++) {
                    NodeInstance n = group.get(i);
                    double current = yOf(ys, n.uid());
                    double nextY = i + 1 < group.size()
                            ? yOf(ys, group.get(i + 1).uid()) : Double.POSITIVE_INFINITY;
                    double desired = medianAllNeighborsY(n.uid(), consumers, producers, ys);
                    if (Double.isNaN(desired)) {
                        prevBottom = current + heightOf(heights, n.uid()); // 孤立节点：不动
                        continue;
                    }
                    double clamped = Math.min(
                            Math.max(desired, prevBottom + GraphLayout.NODE_MARGIN),
                            nextY - GraphLayout.NODE_MARGIN - heightOf(heights, n.uid()));
                    if (clamped != current) {
                        ys.put(n.uid(), clamped);
                        maxDelta = Math.max(maxDelta, Math.abs(clamped - current));
                    }
                    prevBottom = clamped + heightOf(heights, n.uid());
                }
            }
            maxDelta = Math.max(maxDelta, centerLeafFans(byLayer, soleHub, ys, heights));
            if (maxDelta < RELAX_EPSILON) {
                return;
            }
        }
    }

    /**
     * 纯叶扇居中：层内「度数为 1 且共享唯一 hub」的连续节点段作为刚性整体平移，使段的
     * 垂直中心对准 hub 的 y（clamp 不挤压段外邻居；邻居的让位由下一轮逐节点趟完成）。
     * 逐节点贪心对压紧段无自由度（每节点都被邻居卡在原位），扇形会从 hub 单向堆叠到
     * (N−1)×行距 远；刚性居中把最大边距压到 ~(N−1)/2×行距，且居中后的压紧段对逐节点趟
     * 是锁死稳定的。返回本趟最大位移。
     */
    static double centerLeafFans(Map<GraphLayout.ColumnKey, List<NodeInstance>> byLayer,
                                 Map<String, String> soleHub,
                                 Map<String, Double> ys,
                                 Map<String, Float> heights) {
        double maxDelta = 0;
        for (List<NodeInstance> group : byLayer.values()) {
            int size = group.size();
            int i = 0;
            while (i < size) {
                String hub = soleHub.get(group.get(i).uid());
                if (hub == null) {
                    i++;
                    continue;
                }
                int j = i;
                while (j + 1 < size && hub.equals(soleHub.get(group.get(j + 1).uid()))) {
                    j++;
                }
                if (j > i) { // 连续同 hub 纯叶段 ≥ 2
                    double hubY = yOf(ys, hub);
                    double firstY = yOf(ys, group.get(i).uid());
                    double lastY = yOf(ys, group.get(j).uid());
                    double t = hubY - (firstY + lastY) / 2;
                    double lo = i > 0
                            ? yOf(ys, group.get(i - 1).uid()) + heightOf(heights, group.get(i - 1).uid())
                                    + GraphLayout.NODE_MARGIN - firstY
                            : Double.NEGATIVE_INFINITY;
                    double hi = j + 1 < size
                            ? yOf(ys, group.get(j + 1).uid()) - GraphLayout.NODE_MARGIN
                                    - heightOf(heights, group.get(j).uid()) - lastY
                            : Double.POSITIVE_INFINITY;
                    t = Math.min(Math.max(t, lo), hi);
                    if (Math.abs(t) > 1e-6) {
                        for (int k = i; k <= j; k++) {
                            String uid = group.get(k).uid();
                            ys.put(uid, yOf(ys, uid) + t);
                        }
                        maxDelta = Math.max(maxDelta, Math.abs(t));
                    }
                }
                i = j + 1;
            }
        }
        return maxDelta;
    }

    /**
     * 列内按欲望重排：每列内**非语句链**节点（不在 exec_out→exec_in 链上）保持其占据的
     * y 槽位集合不变，槽位按升序重新分配给「按邻居中位数欲望 y 排序」的节点（uid 兜底
     * 确定）。语句链成员不动（执行序是语义，不可被布局重排）；重排后需再跑一轮松弛
     * 恢复节点间距。
     */
    static void reorderColumnsByDesire(Map<GraphLayout.ColumnKey, List<NodeInstance>> byLayer,
                                       Map<String, List<String>> consumers,
                                       Map<String, List<String>> producers,
                                       Map<String, Double> ys,
                                       Map<String, String> seqNext,
                                       Map<String, String> seqPrev) {
        for (List<NodeInstance> group : byLayer.values()) {
            List<Integer> movable = new ArrayList<>();
            for (int i = 0; i < group.size(); i++) {
                String uid = group.get(i).uid();
                if (!seqNext.containsKey(uid) && !seqPrev.containsKey(uid)) {
                    movable.add(i);
                }
            }
            if (movable.size() < 2) {
                continue;
            }
            List<NodeInstance> nodes = new ArrayList<>();
            List<Double> slots = new ArrayList<>();
            for (int i : movable) {
                nodes.add(group.get(i));
                slots.add(yOf(ys, group.get(i).uid()));
            }
            nodes.sort(Comparator
                    .comparingDouble((NodeInstance n) -> {
                        double d = medianAllNeighborsY(n.uid(), consumers, producers, ys);
                        return Double.isNaN(d) ? yOf(ys, n.uid()) : d;
                    })
                    .thenComparing(NodeInstance::uid));
            slots.sort(Comparator.naturalOrder());
            for (int k = 0; k < movable.size(); k++) {
                group.set(movable.get(k), nodes.get(k));
                ys.put(nodes.get(k).uid(), slots.get(k));
            }
        }
    }

    /** 全图边长 Σ|Δy|（悬空端点按 y=0 计，与布局一致）。 */
    static double totalEdgeLength(List<Wire> wires, Map<String, Double> ys) {
        double total = 0;
        for (Wire w : wires) {
            total += Math.abs(yOf(ys, w.to().node()) - yOf(ys, w.from().node()));
        }
        return total;
    }

    /** 全图 y 归一化：统一平移使最小 y = 0（扇居中可能产生负 y；平移不改变边长）。 */
    static void normalizeY(Map<String, Double> ys) {
        double min = ys.values().stream().mapToDouble(Double::doubleValue).min().orElse(0);
        if (min != 0) {
            ys.replaceAll((uid, y) -> y - min);
        }
    }

    /**
     * 全部邻居（双向）y 的中位数；无任何邻居返回 NaN（松弛语义：跳过）。
     * L1 目标（Σ|y−邻居 y|）的单点最优是中位数而非均值——均值会被远端多数派拖拽，
     * 单点移动可能反而增大总边长（实测：大扇形交换成果被均值松弛摧毁，+28551）。
     */
    static double medianAllNeighborsY(String uid, Map<String, List<String>> consumers,
                                      Map<String, List<String>> producers, Map<String, Double> ys) {
        List<Double> neighbors = new ArrayList<>();
        for (String other : consumers.getOrDefault(uid, List.of())) {
            neighbors.add(yOf(ys, other));
        }
        for (String other : producers.getOrDefault(uid, List.of())) {
            neighbors.add(yOf(ys, other));
        }
        return neighbors.isEmpty() ? Double.NaN : median(neighbors);
    }

    /** 中位数（偶数个取中间两值均值——L1 平台期内任何值都最优，取均值保证唯一）。会原地排序。 */
    static double median(List<Double> values) {
        values.sort(Comparator.naturalOrder());
        int mid = values.size() / 2;
        return values.size() % 2 == 1
                ? values.get(mid)
                : (values.get(mid - 1) + values.get(mid)) / 2;
    }

    /** 读取节点 y（悬空线端点按 0 计—— dangling 由 GraphValidator 报告，布局保持全函数）。 */
    static double yOf(Map<String, Double> ys, String uid) {
        return ys.getOrDefault(uid, 0.0);
    }

    /** 读取节点估计高度（缺省按标准高 62）。 */
    static float heightOf(Map<String, Float> heights, String uid) {
        return heights.getOrDefault(uid, GraphLayout.UNKNOWN_NODE_HEIGHT);
    }
}
