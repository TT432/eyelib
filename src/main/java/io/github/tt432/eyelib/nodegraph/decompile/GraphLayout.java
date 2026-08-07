package io.github.tt432.eyelib.nodegraph.decompile;

import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.StickyNote;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 *   <li><b>距离松弛</b>：高斯-塞德尔迭代，每轮先把每个节点向「全部邻居的平均 y」拉拢
 *       （clamp 保持层内顺序与最小行距），再做<b>纯叶扇居中</b>：度数为 1 且共享唯一 hub 的
 *       层内连续段（如 const.string→query.call 的参数扇）作为刚性整体平移到以 hub 的 y
 *       为中心——逐节点贪心下压紧段会从 hub 单向堆叠（边缘成员宁可锁死也不动：居中对其
 *       单边是变差、对总边长和才是变好），而居中后被 clamp 锁死反而成为保持机制。
 *       轮数按图规模给足并带早退。</li>
 *   <li><b>扇形居中重排</b>（实测验证的局部搜索，见 <!--
-->{@link #improveLeafFans}）：重心排序下不同簇仍可能在层内穿插（重心相同按旧 y
 *       交错），把扇形切碎使居中趟锁死。对明显偏离理论最优的纯叶扇执行「按 hub 侧
 *       分区重排 + 扇窗居中压紧」，完整松弛后以全图 Σ|Δy| 实测判定去留——单调下降，
 *       无振荡。</li>
 * </ol>
 * 扇居中平移可能产生负 y，出口处统一平移归一化（全图最小 y = 0，不改变任何边长）。
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

        // 纯叶（度数=1）→ 唯一邻居：图固定，布局期间不变，算一次供松弛与槽位交换共用
        Map<String, String> soleHub = new HashMap<>();
        Map<String, Integer> degree = new HashMap<>();
        for (Wire w : wires) {
            degree.merge(w.from().node(), 1, Integer::sum);
            degree.merge(w.to().node(), 1, Integer::sum);
            soleHub.put(w.from().node(), w.to().node());
            soleHub.put(w.to().node(), w.from().node());
        }
        soleHub.entrySet().removeIf(e -> degree.getOrDefault(e.getKey(), 0) != 1);

        orderByBarycenter(byLayer, consumers, producers, ys);
        relaxDistances(byLayer, consumers, producers, ys, soleHub, nodes.size());
        improveLeafFans(byLayer, soleHub, consumers, producers, wires, ys, nodes.size());
        normalizeY(ys);

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
     * 距离松弛：高斯-塞德尔迭代。每轮两层动作：① 逐节点向「全部邻居（生产者+消费者）
     * 的中位数 y」拉拢（L1 目标的单点最优；clamp 保持层内顺序与最小行距）；② 纯叶扇居中（{@link #centerLeafFans}）。
     * 孤立节点无邻居不参与①（留在均布位）。
     */
    private static void relaxDistances(Map<Integer, List<NodeInstance>> byLayer,
                                       Map<String, List<String>> consumers,
                                       Map<String, List<String>> producers,
                                       Map<String, Double> ys, Map<String, String> soleHub, int nodeCount) {
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
                    double desired = medianAllNeighborsY(n.uid(), consumers, producers, ys);
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
            maxDelta = Math.max(maxDelta, centerLeafFans(byLayer, soleHub, ys));
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
    private static double centerLeafFans(Map<Integer, List<NodeInstance>> byLayer,
                                         Map<String, String> soleHub,
                                         Map<String, Double> ys) {
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
                            ? yOf(ys, group.get(i - 1).uid()) + Y_SPACING - firstY
                            : Double.NEGATIVE_INFINITY;
                    double hi = j + 1 < size
                            ? yOf(ys, group.get(j + 1).uid()) - Y_SPACING - lastY
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

    /** 布局调试日志开关（-Deyelib.layout.debug=true）。 */
    private static final boolean LAYOUT_DEBUG = Boolean.getBoolean("eyelib.layout.debug");
    /** 扇形改进接受阈值：实测全图边长和降幅低于此值则回滚（防微增益空转）。 */
    private static final double MIN_FAN_GAIN = Y_SPACING;
    /** 单次扫描中实测验证的候选上限（每次验证是一次完整松弛，限制最坏耗时）。 */
    private static final int MAX_VERIFY_PER_SCAN = 5;
    /** 扇形改进总轮数上限（每轮接受一次交换且严格降低全图边长和，必终止；此为保险）。 */
    private static final int MAX_FAN_IMPROVE_ROUNDS = 64;

    /**
     * 纯叶扇改进（实测验证的局部搜索）：对明显偏离理论最优的扇（最大边距 > 最优 + 一行距），
     * 执行 {@link #centerFanOnHub}（扇窗居中 + 前后缀按 hub 侧重排压紧）后完整松弛，
     * 以全图 Σ|Δy| 实测判定：降幅超阈值才保留，否则回滚——单调下降，无振荡。
     * 每轮只接受一个交换然后重新扫描（同层其它扇的位置已被重排改变）。
     */
    private static void improveLeafFans(Map<Integer, List<NodeInstance>> byLayer,
                                        Map<String, String> soleHub,
                                        Map<String, List<String>> consumers,
                                        Map<String, List<String>> producers,
                                        List<Wire> wires, Map<String, Double> ys,
                                        int nodeCount) {
        for (int round = 0; round < MAX_FAN_IMPROVE_ROUNDS; round++) {
            if (!tryOneFanCentering(byLayer, soleHub, consumers, producers, wires, ys, nodeCount)) {
                return;
            }
        }
    }

    /**
     * 全图扫描候选扇并逐个实测（规模大优先，同则 hub uid 升序）；接受其一即返回 true。
     * 候选条件：最大边距超过「理论最优 + 一行距」（理论最优 = 扇形压紧居中于 hub）。
     */
    private static boolean tryOneFanCentering(Map<Integer, List<NodeInstance>> byLayer,
                                              Map<String, String> soleHub,
                                              Map<String, List<String>> consumers,
                                              Map<String, List<String>> producers,
                                              List<Wire> wires, Map<String, Double> ys, int nodeCount) {
        for (List<NodeInstance> group : byLayer.values()) {
            // hub → 该层内扇成员（LinkedHashMap 保证遍历确定）
            Map<String, List<NodeInstance>> fans = new LinkedHashMap<>();
            for (NodeInstance n : group) {
                String hub = soleHub.get(n.uid());
                if (hub != null) {
                    fans.computeIfAbsent(hub, k -> new ArrayList<>()).add(n);
                }
            }
            List<Map.Entry<String, List<NodeInstance>>> candidates = new ArrayList<>();
            for (Map.Entry<String, List<NodeInstance>> e : fans.entrySet()) {
                int k = e.getValue().size();
                if (k < 2) {
                    continue;
                }
                double hubY = yOf(ys, e.getKey());
                double maxEdge = 0;
                for (NodeInstance n : e.getValue()) {
                    maxEdge = Math.max(maxEdge, Math.abs(yOf(ys, n.uid()) - hubY));
                }
                double optimal = (k - 1) / 2.0 * Y_SPACING;
                if (maxEdge > optimal + Y_SPACING) {
                    candidates.add(e);
                }
            }
            candidates.sort(Comparator
                    .comparingInt((Map.Entry<String, List<NodeInstance>> e) -> -e.getValue().size())
                    .thenComparing(Map.Entry.comparingByKey()));
            int verified = 0;
            for (Map.Entry<String, List<NodeInstance>> candidate : candidates) {
                if (verified++ >= MAX_VERIFY_PER_SCAN) {
                    break;
                }
                String hub = candidate.getKey();
                double before = totalEdgeLength(wires, ys);
                List<NodeInstance> savedOrder = new ArrayList<>(group);
                Map<String, Double> savedYs = new HashMap<>(ys);
                centerFanOnHub(group, hub, soleHub, ys);
                relaxDistances(byLayer, consumers, producers, ys, soleHub, nodeCount);
                double after = totalEdgeLength(wires, ys);
                if (LAYOUT_DEBUG) {
                    org.slf4j.LoggerFactory.getLogger(GraphLayout.class).info(
                            "[layout] fan-center hub={} k={} before={} after={} {}",
                            hub, candidate.getValue().size(), (int) before, (int) after,
                            after < before - MIN_FAN_GAIN ? "ACCEPT" : "REJECT");
                }
                if (after < before - MIN_FAN_GAIN) {
                    return true;
                }
                group.clear();
                group.addAll(savedOrder);
                ys.clear();
                ys.putAll(savedYs);
            }
        }
        return false;
    }

    /**
     * 扇窗居中重排：层内节点重排为「前缀（y &lt; hubY 的非扇成员）+ 扇 + 后缀」，
     * 扇成员压紧在 hubY 居中窗口，前缀/后缀分别向上/向下压紧。逐节点松弛在压紧层里
     * 没有自由度（扇会锁回原位），必须重排 + 直接放置才能让扇到达居中位置；
     * 是否保留由全图边长和实测判定（见 {@link #tryOneFanCentering}）。
     */
    private static void centerFanOnHub(List<NodeInstance> group, String hub,
                                       Map<String, String> soleHub, Map<String, Double> ys) {
        double hubY = yOf(ys, hub);
        List<NodeInstance> fan = new ArrayList<>();
        List<NodeInstance> prefix = new ArrayList<>();
        List<NodeInstance> suffix = new ArrayList<>();
        for (NodeInstance n : group) {
            if (hub.equals(soleHub.get(n.uid()))) {
                fan.add(n);
            } else if (yOf(ys, n.uid()) < hubY) {
                prefix.add(n);
            } else {
                suffix.add(n);
            }
        }
        int k = fan.size();
        double startY = hubY - (k - 1) / 2.0 * Y_SPACING;
        double y = startY - Y_SPACING;
        for (int i = prefix.size() - 1; i >= 0; i--) {
            ys.put(prefix.get(i).uid(), y);
            y -= Y_SPACING;
        }
        for (int i = 0; i < k; i++) {
            ys.put(fan.get(i).uid(), startY + i * Y_SPACING);
        }
        y = startY + k * Y_SPACING;
        for (NodeInstance n : suffix) {
            ys.put(n.uid(), y);
            y += Y_SPACING;
        }
        group.clear();
        group.addAll(prefix);
        group.addAll(fan);
        group.addAll(suffix);
    }

    /** 全图边长 Σ|Δy|（悬空端点按 y=0 计，与布局一致）。 */
    private static double totalEdgeLength(List<Wire> wires, Map<String, Double> ys) {
        double total = 0;
        for (Wire w : wires) {
            total += Math.abs(yOf(ys, w.to().node()) - yOf(ys, w.from().node()));
        }
        return total;
    }

    /** 全图 y 归一化：统一平移使最小 y = 0（扇居中可能产生负 y；平移不改变边长）。 */
    private static void normalizeY(Map<String, Double> ys) {
        double min = ys.values().stream().mapToDouble(Double::doubleValue).min().orElse(0);
        if (min != 0) {
            ys.replaceAll((uid, y) -> y - min);
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

    /**
     * 全部邻居（双向）y 的中位数；无任何邻居返回 NaN（松弛语义：跳过）。
     * L1 目标（Σ|y−邻居 y|）的单点最优是中位数而非均值——均值会被远端多数派拖拽，
     * 单点移动可能反而增大总边长（实测：大扇形交换成果被均值松弛摧毁，+28551）。
     */
    private static double medianAllNeighborsY(String uid, Map<String, List<String>> consumers,
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
    private static double median(List<Double> values) {
        values.sort(Comparator.naturalOrder());
        int mid = values.size() / 2;
        return values.size() % 2 == 1
                ? values.get(mid)
                : (values.get(mid - 1) + values.get(mid)) / 2;
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
