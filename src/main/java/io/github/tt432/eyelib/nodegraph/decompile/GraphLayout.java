package io.github.tt432.eyelib.nodegraph.decompile;

import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
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
 * 分层自动布局（规格 nodegraph-workbench §W2）：<b>到汇最长路径定 x 层；层内以 DFS 先序定序、
 * 距离松弛定 y</b>。
 *
 * <p>坐标遵循编辑器既有约定（versions/1.20.1/run/config/eyelib/nodegraph/ng_smoke.json）：
 * 数据流方向 x 左→右（生产者左、消费者右，汇——装配根——在最右列），列距 300；
 * 垂直间距按节点估计高度逐对计算（{@code 上节点高 + 边距}，默认行距 110 = 标准高 62 + 48）。
 * 无连线的孤立节点（如声明表 ref.*）放在汇左一列。
 *
 * <p>算法阶段（都是确定性纯函数）：
 * <ol>
 *   <li><b>层内定序 = 汇出发反向 DFS 先序</b>。表达式链在上游几乎全是独占树（唯一消费者），
 *       DFS 先序天然让链/子树成员在层内连续——实测（悦灵 812 节点，2026-08-07）比重心排序
 *       极端边数 29→9：重心排序每趟重置均布 y 会破坏链连续性（曾引以为傲的「簇邻接」收益
 *       远小于链断裂损失）。</li>
 *   <li><b>巨扇分列</b>（{@link #splitGiantFans}）：「唯一消费者同为某 hub」或「唯一生产者
 *       同为某 hub」的成员群超过单列上限时切成多列（不限度数，带表达式链的 rc.root 也算）。
 *       分列只缩冠幅占用的列高、不强行移动成员——成员位置由链锚定，强挪会把长边挪进链里
 *       （实测 Σ +48%）。</li>
 *   <li><b>距离松弛</b>：高斯-塞德尔迭代，每轮先把每个节点向「全部邻居的中位数 y」拉拢
 *       （L1 目标的单点最优；clamp 保持层内顺序与节点高度间距），再做<b>纯叶扇居中</b>
 *       （度数为 1 且共享唯一 hub 的层内连续段作为刚性整体平移到 hub y——压紧段对逐节点
 *       趟无自由度，单向堆叠就靠这个拆开）。轮数按图规模给足并带早退。</li>
 *   <li><b>扇形居中重排</b>（{@link #improveLeafFans}，实测验收的局部搜索）：对明显偏离
 *       理论最优的纯叶扇执行「按 hub 侧分区重排 + 扇窗居中压紧」，完整松弛后以全图 Σ|Δy|
 *       实测判定去留——单调下降，无振荡。</li>
 *   <li><b>hub 极小极大定位</b>（{@link #centerHubsOnFanSpan}）：把大扇 hub 移到扇成员 y 跨
 *       中心（clamp 不撞列内邻居）。松弛的中位数投票把 hub 停在 L1 最优点（偏向成员密集侧），
 *       扇尾成员的边距可达扇跨全长；移到跨中把扇的最大边压到跨度一半，牺牲的 Σ 很小
 *       （hub 移动只影响其自身边）。扇跨度本身由成员的链锚定，不可压缩。</li>
 * </ol>
 * 扇居中平移可能产生负 y，出口处统一平移归一化（全图最小 y = 0，不改变任何边长）。
 * 环只做防御（层按 0 计），环本身由 GraphValidator.CYCLE 报告。
 *
 * <p><b>已证伪的改进方向（悦灵实测，仿真与实机一致）</b>——勿重试：
 * ① 巨扇成员强制居中（整列居中/实测验收居中）：链跟不上，长边从扇边挪进链边（Σ +48%
 * 或 max +44%）；且整列居中与逐节点松弛恒幅振荡，不能进早退循环；
 * ② 远邻 blend（median 向 >2000 的邻居偏移）：Σ 与极端边数齐升；
 * ③ 松弛后再重心排序交替：收敛到同一盆域，无收益。
 */
public final class GraphLayout {
    private GraphLayout() {
    }

    /** 列距（同 ng_smoke.json 的 300）。 */
    public static final float X_SPACING = 300f;
    /** 默认行距：两个「标准高度」（62px，如 const.number）节点加上边距后的间距。
     *  实际垂直间距按节点估计高度逐对计算（{@link #estimateHeight}），本常量只作
     *  阈值/增益单位与未知类型节点的等效行距。 */
    public static final float Y_SPACING = 110f;
    /** 节点间垂直边距：间距 = 上节点高 + MARGIN。62（标准高）+ 48 = 110 = 旧固定行距，
     *  默认节点的视觉间距与旧版一致。 */
    private static final float NODE_MARGIN = 48f;

    /** 松弛早退阈值：单轮最大位移低于此值即视为收敛。 */
    private static final double RELAX_EPSILON = 0.5;
    /** 单列扇成员上限：超过则拆成并列子列（见 {@link #splitGiantFans}）。 */
    private static final int MAX_FAN_LEAVES_PER_COLUMN = 16;
    /** 极小极大定位的最小扇规模：小于此数的扇 hub 交给中位数投票即可。 */
    private static final int MIN_MINIMAX_FAN_SIZE = 4;

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

        // 同层分组（层号升序 = 汇→源，确定性遍历），按 DFS 先序排定层内顺序
        Map<ColumnKey, List<NodeInstance>> byLayer = new TreeMap<>();
        for (NodeInstance node : nodes) {
            int layer = wiredNodes.contains(node.uid()) ? layers.get(node.uid()) : isolatedLayer;
            byLayer.computeIfAbsent(new ColumnKey(layer, 0), k -> new ArrayList<>()).add(node);
        }
        byLayer.values().forEach(group -> group.sort(Comparator
                .comparingInt((NodeInstance n) -> order.getOrDefault(n.uid(), Integer.MAX_VALUE))
                .thenComparing(NodeInstance::uid)));

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

        // 唯一消费者/唯一生产者（去重后计；同两节点间多根线会产生重复项，必须 distinct）：
        // 巨扇分列与 hub 极小极大定位用
        Map<String, String> soleConsumer = new HashMap<>();
        Map<String, String> soleProducer = new HashMap<>();
        for (Map.Entry<String, List<String>> e : consumers.entrySet()) {
            List<String> distinct = e.getValue().stream().distinct().toList();
            if (distinct.size() == 1) {
                soleConsumer.put(e.getKey(), distinct.get(0));
            }
        }
        for (Map.Entry<String, List<String>> e : producers.entrySet()) {
            List<String> distinct = e.getValue().stream().distinct().toList();
            if (distinct.size() == 1) {
                soleProducer.put(e.getKey(), distinct.get(0));
            }
        }

        // 巨扇分列（在赋 y 之前：子列独立从 0 起均布）
        splitGiantFans(byLayer, soleConsumer, soleProducer);

        // 节点估计高度（LDLib2 编辑器实测标定，见 {@link #estimateHeight}）：垂直间距按
        // 「上节点高 + 边距」逐对计算——固定行距会让 16 个 arg 的 query.call（318px）
        // 这类高节点与邻居重叠（用户实机截图实证 2026-08-07）
        Map<String, Float> heights = new HashMap<>();
        for (NodeInstance n : nodes) {
            heights.put(n.uid(), estimateHeight(n));
        }

        // 初始均布 y（按累计高度）
        Map<String, Double> ys = new HashMap<>();
        byLayer.values().forEach(group -> {
            double y = 0;
            for (NodeInstance n : group) {
                ys.put(n.uid(), y);
                y += heightOf(heights, n.uid()) + NODE_MARGIN;
            }
        });

        relaxDistances(byLayer, consumers, producers, ys, soleHub, heights, nodes.size());
        improveLeafFans(byLayer, soleHub, consumers, producers, wires, ys, heights, nodes.size());
        centerHubsOnFanSpan(byLayer, soleConsumer, soleProducer, ys, wires, heights);
        normalizeY(ys);

        // 列 x 槽位：从右向左按（层号升序、层内 sub 降序）逐个占槽。无分列时与
        // （maxLayer−layer）×列距 严格一致（最长路径保证 0..maxLayer 每层都有节点，无空槽）；
        // 有分列时子列占完整列槽，汇侧列整体右移让位——边仍左→右，无碰撞
        List<Map.Entry<ColumnKey, List<NodeInstance>>> rightToLeft = new ArrayList<>(byLayer.entrySet());
        rightToLeft.sort(Comparator
                .comparingInt((Map.Entry<ColumnKey, List<NodeInstance>> e) -> e.getKey().layer())
                .thenComparing(Comparator
                        .comparingInt((Map.Entry<ColumnKey, List<NodeInstance>> e) -> e.getKey().sub())
                        .reversed()));
        Map<String, Integer> xIndexOfUid = new HashMap<>();
        for (int i = 0; i < rightToLeft.size(); i++) {
            int xIndex = rightToLeft.size() - 1 - i;
            for (NodeInstance n : rightToLeft.get(i).getValue()) {
                xIndexOfUid.put(n.uid(), xIndex);
            }
        }
        List<NodeInstance> out = new ArrayList<>(nodes.size());
        for (NodeInstance n : nodes) {
            Integer xIndex = xIndexOfUid.get(n.uid());
            float x = xIndex == null
                    ? (maxLayer - isolatedLayer) * X_SPACING
                    : xIndex * X_SPACING;
            float y = ys.getOrDefault(n.uid(), 0.0).floatValue();
            out.add(new NodeInstance(n.uid(), n.type(), x, y, n.options(), n.constants()));
        }
        return out;
    }

    /** 列键：layer = 到汇最长路径层；sub = 巨扇分列的子列偏移（0=主列，负=左，正=右）。 */
    private record ColumnKey(int layer, int sub) implements Comparable<ColumnKey> {
        @Override
        public int compareTo(ColumnKey other) {
            int c = Integer.compare(layer, other.layer);
            return c != 0 ? c : Integer.compare(sub, other.sub);
        }
    }

    /**
     * 巨扇分列：列内「唯一消费者同为某 hub」（生产侧扇）或「唯一生产者同为某 hub」
     * （消费侧扇）的成员数超过 {@link #MAX_FAN_LEAVES_PER_COLUMN} 时，整扇按当前组内
     * 顺序切成 ⌈k/上限⌉ 块，每块各成一个子列（生产侧子列在主列右侧=靠近 hub，
     * 消费侧在左侧）。分列的意义是释放列高让每块的链能各自贴近 hub——
     * 成员位置仍由松弛决定（不强行居中：链跟不上，实测 Σ +48%）。
     * 同层多扇按序占用子列序号，全程确定。
     */
    private static void splitGiantFans(Map<ColumnKey, List<NodeInstance>> byLayer,
                                       Map<String, String> soleConsumer,
                                       Map<String, String> soleProducer) {
        for (Map.Entry<ColumnKey, List<NodeInstance>> entry : new ArrayList<>(byLayer.entrySet())) {
            List<NodeInstance> group = entry.getValue();
            Map<String, List<NodeInstance>> producerSide = new LinkedHashMap<>();
            Map<String, List<NodeInstance>> consumerSide = new LinkedHashMap<>();
            for (NodeInstance n : group) {
                String consumer = soleConsumer.get(n.uid());
                if (consumer != null) {
                    producerSide.computeIfAbsent(consumer, k -> new ArrayList<>()).add(n);
                }
                String producer = soleProducer.get(n.uid());
                if (producer != null && !producer.equals(consumer)) {
                    consumerSide.computeIfAbsent(producer, k -> new ArrayList<>()).add(n);
                }
            }
            int nextRight = 0;
            int nextLeft = 0;
            for (Map.Entry<String, List<NodeInstance>> fan : producerSide.entrySet()) {
                nextRight = moveFanChunksToSubColumns(byLayer, entry.getKey(), group,
                        fan.getKey(), fan.getValue(), 1, nextRight);
            }
            for (Map.Entry<String, List<NodeInstance>> fan : consumerSide.entrySet()) {
                nextLeft = moveFanChunksToSubColumns(byLayer, entry.getKey(), group,
                        fan.getKey(), fan.getValue(), -1, nextLeft);
            }
            if (group.isEmpty()) {
                byLayer.remove(entry.getKey());
            }
        }
    }

    /**
     * 把超过上限的扇按块搬入子列（direction &gt; 0 占正序号=主列右侧，反之左侧）；
     * 已被先前扇搬走的成员跳过。返回该方向下一个可用子列序号。
     */
    private static int moveFanChunksToSubColumns(Map<ColumnKey, List<NodeInstance>> byLayer,
                                                 ColumnKey baseKey, List<NodeInstance> group,
                                                 String hub, List<NodeInstance> members,
                                                 int direction, int nextSub) {
        List<NodeInstance> present = new ArrayList<>(members);
        present.retainAll(group);
        if (present.size() <= MAX_FAN_LEAVES_PER_COLUMN) {
            return nextSub;
        }
        if (LAYOUT_DEBUG) {
            org.slf4j.LoggerFactory.getLogger(GraphLayout.class).info(
                    "[layout] split fan hub={} k={} chunks={} dir={}",
                    hub, present.size(),
                    (present.size() + MAX_FAN_LEAVES_PER_COLUMN - 1) / MAX_FAN_LEAVES_PER_COLUMN,
                    direction);
        }
        for (int index = 0; index < present.size(); index += MAX_FAN_LEAVES_PER_COLUMN) {
            List<NodeInstance> chunk = new ArrayList<>(present.subList(
                    index, Math.min(index + MAX_FAN_LEAVES_PER_COLUMN, present.size())));
            int sub = direction > 0 ? ++nextSub : --nextSub;
            byLayer.put(new ColumnKey(baseKey.layer(), sub), chunk);
            group.removeAll(chunk);
        }
        return nextSub;
    }

    /**
     * hub 极小极大定位（实测判定）：把大扇（≥{@link #MIN_MINIMAX_FAN_SIZE} 成员）的 hub 移向扇
     * 成员 y 跨度中心（clamp 不撞列内邻居）。松弛的中位数投票把 hub 停在 L1 最优点（偏向成员
     * 密集侧），扇尾成员边距可达扇跨全长（悦灵 rcr63→root 5417）；移到跨中把扇最大边压到
     * 跨度一半。移动以「hub 全部边的最大边距」实测判定：改善不足一个行距则回滚——
     * 小扇/已居中的 hub 移动只会把边距转移给其非扇边（如 hub→汇），实测判定挡住。
     * 在 improveLeafFans 之后运行，不再松弛——再松弛会把 hub 拉回中位数最优点。
     */
    private static void centerHubsOnFanSpan(Map<ColumnKey, List<NodeInstance>> byLayer,
                                            Map<String, String> soleConsumer,
                                            Map<String, String> soleProducer,
                                            Map<String, Double> ys, List<Wire> wires,
                                            Map<String, Float> heights) {
        // hub → 扇成员
        Map<String, List<String>> fanOf = new HashMap<>();
        for (Map.Entry<String, String> e : soleConsumer.entrySet()) {
            fanOf.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        for (Map.Entry<String, String> e : soleProducer.entrySet()) {
            if (!e.getValue().equals(soleConsumer.get(e.getKey()))) {
                fanOf.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
            }
        }
        Map<String, List<String>> neighbors = new HashMap<>();
        for (Wire w : wires) {
            neighbors.computeIfAbsent(w.from().node(), k -> new ArrayList<>()).add(w.to().node());
            neighbors.computeIfAbsent(w.to().node(), k -> new ArrayList<>()).add(w.from().node());
        }
        // 汇侧列先处理（TreeMap 层号升序遍历）
        for (List<NodeInstance> group : byLayer.values()) {
            for (int i = 0; i < group.size(); i++) {
                String uid = group.get(i).uid();
                List<String> fan = fanOf.get(uid);
                if (fan == null || fan.size() < MIN_MINIMAX_FAN_SIZE) {
                    continue;
                }
                double lo = Double.POSITIVE_INFINITY;
                double hi = Double.NEGATIVE_INFINITY;
                for (String member : fan) {
                    lo = Math.min(lo, yOf(ys, member));
                    hi = Math.max(hi, yOf(ys, member));
                }
                double target = (lo + hi) / 2;
                double loBound = i > 0
                        ? yOf(ys, group.get(i - 1).uid()) + heightOf(heights, group.get(i - 1).uid()) + NODE_MARGIN
                        : Double.NEGATIVE_INFINITY;
                double hiBound = i + 1 < group.size()
                        ? yOf(ys, group.get(i + 1).uid()) - NODE_MARGIN - heightOf(heights, uid)
                        : Double.POSITIVE_INFINITY;
                double oldY = yOf(ys, uid);
                double newY = Math.min(Math.max(target, loBound), hiBound);
                if (newY == oldY) {
                    continue;
                }
                double before = maxEdgeToNeighbors(uid, neighbors, ys);
                ys.put(uid, newY);
                double after = maxEdgeToNeighbors(uid, neighbors, ys);
                if (after > before - Y_SPACING) {
                    ys.put(uid, oldY); // 改善不足一个行距 → 回滚
                }
            }
        }
    }

    /** 节点全部边（双向）的最大垂直边距；无邻居为 0。 */
    private static double maxEdgeToNeighbors(String uid, Map<String, List<String>> neighbors,
                                             Map<String, Double> ys) {
        double max = 0;
        for (String other : neighbors.getOrDefault(uid, List.of())) {
            max = Math.max(max, Math.abs(yOf(ys, uid) - yOf(ys, other)));
        }
        return max;
    }

    /** 便签定位：图最右列右侧一列，自上而下堆叠（行距取默认行距与便签自身高度+边距的较大者）。 */
    public static List<StickyNote> placeStickyNotes(List<StickyNote> notes, List<NodeInstance> laidOutNodes) {
        float maxX = 0;
        for (NodeInstance n : laidOutNodes) {
            maxX = Math.max(maxX, n.x());
        }
        List<StickyNote> out = new ArrayList<>(notes.size());
        float y = 0;
        for (StickyNote n : notes) {
            out.add(new StickyNote(n.uid(), n.text(), maxX + X_SPACING, y,
                    n.width(), n.height(), n.color()));
            y += Math.max(Y_SPACING, n.height() + 10f);
        }
        return out;
    }

    /**
     * 距离松弛：高斯-塞德尔迭代。每轮两层动作：① 逐节点向「全部邻居（生产者+消费者）
     * 的中位数 y」拉拢（L1 目标的单点最优；clamp 保持层内顺序与节点高度间距）；② 纯叶扇居中（{@link #centerLeafFans}）。
     * 孤立节点无邻居不参与①（留在均布位）。
     */
    private static void relaxDistances(Map<ColumnKey, List<NodeInstance>> byLayer,
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
                            Math.max(desired, prevBottom + NODE_MARGIN),
                            nextY - NODE_MARGIN - heightOf(heights, n.uid()));
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
    private static double centerLeafFans(Map<ColumnKey, List<NodeInstance>> byLayer,
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
                                    + NODE_MARGIN - firstY
                            : Double.NEGATIVE_INFINITY;
                    double hi = j + 1 < size
                            ? yOf(ys, group.get(j + 1).uid()) - NODE_MARGIN
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
    private static void improveLeafFans(Map<ColumnKey, List<NodeInstance>> byLayer,
                                        Map<String, String> soleHub,
                                        Map<String, List<String>> consumers,
                                        Map<String, List<String>> producers,
                                        List<Wire> wires, Map<String, Double> ys,
                                        Map<String, Float> heights,
                                        int nodeCount) {
        for (int round = 0; round < MAX_FAN_IMPROVE_ROUNDS; round++) {
            if (!tryOneFanCentering(byLayer, soleHub, consumers, producers, wires, ys, heights, nodeCount)) {
                return;
            }
        }
    }

    /**
     * 全图扫描候选扇并逐个实测（规模大优先，同则 hub uid 升序）；接受其一即返回 true。
     * 候选条件：最大边距超过「理论最优 + 一行距」（理论最优 = 扇形压紧居中于 hub）。
     */
    private static boolean tryOneFanCentering(Map<ColumnKey, List<NodeInstance>> byLayer,
                                              Map<String, String> soleHub,
                                              Map<String, List<String>> consumers,
                                              Map<String, List<String>> producers,
                                              List<Wire> wires, Map<String, Double> ys,
                                              Map<String, Float> heights, int nodeCount) {
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
                double window = -NODE_MARGIN; // 扇窗像素高 = Σ成员高 + 边距×(k−1)
                for (NodeInstance n : e.getValue()) {
                    maxEdge = Math.max(maxEdge, Math.abs(yOf(ys, n.uid()) - hubY));
                    window += heightOf(heights, n.uid()) + NODE_MARGIN;
                }
                double optimal = window / 2;
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
                centerFanOnHub(group, hub, soleHub, ys, heights);
                relaxDistances(byLayer, consumers, producers, ys, soleHub, heights, nodeCount);
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
                                       Map<String, String> soleHub, Map<String, Double> ys,
                                       Map<String, Float> heights) {
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
        // 扇窗像素高 = Σ成员高 + 边距×(k−1)；以 hub 中心居中
        double window = -NODE_MARGIN;
        for (NodeInstance n : fan) {
            window += heightOf(heights, n.uid()) + NODE_MARGIN;
        }
        double startY = hubY + heightOf(heights, hub) / 2 - window / 2;
        double y = startY - NODE_MARGIN;
        for (int i = prefix.size() - 1; i >= 0; i--) {
            y -= heightOf(heights, prefix.get(i).uid());
            ys.put(prefix.get(i).uid(), y);
            y -= NODE_MARGIN;
        }
        y = startY;
        for (NodeInstance n : fan) {
            ys.put(n.uid(), y);
            y += heightOf(heights, n.uid()) + NODE_MARGIN;
        }
        for (NodeInstance n : suffix) {
            ys.put(n.uid(), y);
            y += heightOf(heights, n.uid()) + NODE_MARGIN;
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

    /** 读取节点估计高度（缺省按标准高 62）。 */
    private static float heightOf(Map<String, Float> heights, String uid) {
        return heights.getOrDefault(uid, UNKNOWN_NODE_HEIGHT);
    }

    /** 未知类型节点的等效高度（= const.number 实测高 62，配合 {@link #NODE_MARGIN} 保持默认行距 110）。 */
    private static final float UNKNOWN_NODE_HEIGHT = 62f;
    /** variable 节点（变量芯片）实测高 18，估计取 20。 */
    private static final float VARIABLE_NODE_HEIGHT = 20f;
    /** 内嵌资产预览的 ref 节点（ref.texture/ref.geometry，64px 预览）实测高 172，估计取 174。 */
    private static final float REF_PREVIEW_NODE_HEIGHT = 174f;
    /** 无预览的 ref 节点实测高 94，估计取 96。 */
    private static final float REF_NODE_HEIGHT = 96f;
    /**
     * 布局用空子图解析器：布局只需端口数量，子图调用节点的动态端口按静态端口计
     * （低估只多留空白，不会重叠）。
     */
    private static final NodeType.SubgraphResolver NO_SUBGRAPH = subgraphName -> java.util.Optional.empty();

    /**
     * 节点渲染高度估计（图坐标单位）：{@code 28 + 16×max(入,出) + 18×选项行数}。
     * 常数经 LDLib2 编辑器实测标定（2026-08-07，1.20.1 fork 2.2.27，812 节点全量采样，
     * 最大误差 +2，宁高估不低估——高估只多留白，低估会重叠）：
     * query.call(arg_count=16) 实测 318 = 28+16×16+18×2；entity.root(11 入 1 选项) 实测 222 精确命中。
     * 选项文本不换行（单行省略），与高度无关——实测同类型 len=29 与 len=40 高度相同。
     * 特例：ref.texture / ref.geometry 内嵌 64px 资产预览（实测 172）；
     * 其余 ref.* 无预览但选项区较高（实测 94）。
     */
    private static float estimateHeight(NodeInstance n) {
        switch (n.type()) {
            case "variable":
                return VARIABLE_NODE_HEIGHT;
            case "ref.texture":
            case "ref.geometry":
                return REF_PREVIEW_NODE_HEIGHT;
            case "ref.animation":
            case "ref.material":
            case "ref.ac":
            case "ref.rc":
                return REF_NODE_HEIGHT;
            default:
                break;
        }
        java.util.Optional<NodeType> typeOpt = NodeTypes.get(n.type());
        if (typeOpt.isEmpty()) {
            return UNKNOWN_NODE_HEIGHT;
        }
        NodeType type = typeOpt.get();
        int rows = Math.max(
                type.inputsOf(n, NO_SUBGRAPH).size(),
                type.outputsOf(n, NO_SUBGRAPH).size());
        return 28 + 16f * rows + 18f * type.options().size();
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
