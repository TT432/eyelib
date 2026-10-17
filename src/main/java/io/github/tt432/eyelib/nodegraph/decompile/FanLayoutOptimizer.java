package io.github.tt432.eyelib.nodegraph.decompile;

import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 扇区优化：巨扇分列（列高上限）、hub 极小极大定位（扇跨度中心）、纯叶扇改进的
 * 实测验证局部搜索。全部方法只通过参数交互（列分组、扇关系表、y 映射、高度表），
 * 无实例状态；松弛由 {@link GraphRelaxer#relaxDistances} 承担。
 *
 * <p>坐标常量沿用 {@link GraphLayout#X_SPACING}、{@link GraphLayout#Y_SPACING}、
 * {@link GraphLayout#NODE_MARGIN}。
 */
final class FanLayoutOptimizer {
    private FanLayoutOptimizer() {
    }

    /** 单列扇成员上限：超过则拆成并列子列（见 {@link #splitGiantFans}）。 */
    private static final int MAX_FAN_LEAVES_PER_COLUMN = 16;
    /** 极小极大定位的最小扇规模：小于此数的扇 hub 交给中位数投票即可。 */
    private static final int MIN_MINIMAX_FAN_SIZE = 4;

    /** 布局调试日志开关（-Deyelib.layout.debug=true）。 */
    private static final boolean LAYOUT_DEBUG = Boolean.getBoolean("eyelib.layout.debug");
    /** 扇形改进接受阈值：实测全图边长和降幅低于此值则回滚（防微增益空转）。 */
    private static final double MIN_FAN_GAIN = GraphLayout.Y_SPACING;
    /** 单次扫描中实测验证的候选上限（每次验证是一次完整松弛，限制最坏耗时）。 */
    private static final int MAX_VERIFY_PER_SCAN = 5;
    /** 扇形改进总轮数上限（每轮接受一次交换且严格降低全图边长和，必终止；此为保险）。 */
    private static final int MAX_FAN_IMPROVE_ROUNDS = 64;

    /**
     * 巨扇分列：列内「唯一消费者同为某 hub」（生产侧扇）或「唯一生产者同为某 hub」
     * （消费侧扇）的成员数超过 {@link #MAX_FAN_LEAVES_PER_COLUMN} 时，整扇按当前组内
     * 顺序切成 ⌈k/上限⌉ 块，每块各成一个子列（生产侧子列在主列右侧=靠近 hub，
     * 消费侧在左侧）。分列的意义是释放列高让每块的链能各自贴近 hub——
     * 成员位置仍由松弛决定（不强行居中：链跟不上，实测 Σ +48%）。
     * 同层多扇按序占用子列序号，全程确定。
     */
    static void splitGiantFans(Map<GraphLayout.ColumnKey, List<NodeInstance>> byLayer,
                               Map<String, String> soleConsumer,
                               Map<String, String> soleProducer) {
        for (Map.Entry<GraphLayout.ColumnKey, List<NodeInstance>> entry
                : new ArrayList<>(byLayer.entrySet())) {
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
    static int moveFanChunksToSubColumns(Map<GraphLayout.ColumnKey, List<NodeInstance>> byLayer,
                                         GraphLayout.ColumnKey baseKey, List<NodeInstance> group,
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
            byLayer.put(new GraphLayout.ColumnKey(baseKey.layer(), sub), chunk);
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
    static void centerHubsOnFanSpan(Map<GraphLayout.ColumnKey, List<NodeInstance>> byLayer,
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
                    lo = Math.min(lo, GraphRelaxer.yOf(ys, member));
                    hi = Math.max(hi, GraphRelaxer.yOf(ys, member));
                }
                double target = (lo + hi) / 2;
                double loBound = i > 0
                        ? GraphRelaxer.yOf(ys, group.get(i - 1).uid())
                                + GraphRelaxer.heightOf(heights, group.get(i - 1).uid())
                                + GraphLayout.NODE_MARGIN
                        : Double.NEGATIVE_INFINITY;
                double hiBound = i + 1 < group.size()
                        ? GraphRelaxer.yOf(ys, group.get(i + 1).uid()) - GraphLayout.NODE_MARGIN
                                - GraphRelaxer.heightOf(heights, uid)
                        : Double.POSITIVE_INFINITY;
                double oldY = GraphRelaxer.yOf(ys, uid);
                double newY = Math.min(Math.max(target, loBound), hiBound);
                if (newY == oldY) {
                    continue;
                }
                double before = maxEdgeToNeighbors(uid, neighbors, ys);
                ys.put(uid, newY);
                double after = maxEdgeToNeighbors(uid, neighbors, ys);
                if (after > before - GraphLayout.Y_SPACING) {
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
            max = Math.max(max, Math.abs(GraphRelaxer.yOf(ys, uid) - GraphRelaxer.yOf(ys, other)));
        }
        return max;
    }

    /**
     * 纯叶扇改进（实测验证的局部搜索）：对明显偏离理论最优的扇（最大边距 > 最优 + 一行距），
     * 执行 {@link #centerFanOnHub}（扇窗居中 + 前后缀按 hub 侧重排压紧）后完整松弛，
     * 以全图 Σ|Δy| 实测判定：降幅超阈值才保留，否则回滚——单调下降，无振荡。
     * 每轮只接受一个交换然后重新扫描（同层其它扇的位置已被重排改变）。
     */
    static void improveLeafFans(Map<GraphLayout.ColumnKey, List<NodeInstance>> byLayer,
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
    private static boolean tryOneFanCentering(Map<GraphLayout.ColumnKey, List<NodeInstance>> byLayer,
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
                double hubY = GraphRelaxer.yOf(ys, e.getKey());
                double maxEdge = 0;
                double window = -GraphLayout.NODE_MARGIN; // 扇窗像素高 = Σ成员高 + 边距×(k−1)
                for (NodeInstance n : e.getValue()) {
                    maxEdge = Math.max(maxEdge,
                            Math.abs(GraphRelaxer.yOf(ys, n.uid()) - hubY));
                    window += GraphRelaxer.heightOf(heights, n.uid()) + GraphLayout.NODE_MARGIN;
                }
                double optimal = window / 2;
                if (maxEdge > optimal + GraphLayout.Y_SPACING) {
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
                double before = GraphRelaxer.totalEdgeLength(wires, ys);
                List<NodeInstance> savedOrder = new ArrayList<>(group);
                Map<String, Double> savedYs = new HashMap<>(ys);
                centerFanOnHub(group, hub, soleHub, ys, heights);
                GraphRelaxer.relaxDistances(byLayer, consumers, producers, ys, soleHub, heights, nodeCount);
                double after = GraphRelaxer.totalEdgeLength(wires, ys);
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
        double hubY = GraphRelaxer.yOf(ys, hub);
        List<NodeInstance> fan = new ArrayList<>();
        List<NodeInstance> prefix = new ArrayList<>();
        List<NodeInstance> suffix = new ArrayList<>();
        for (NodeInstance n : group) {
            if (hub.equals(soleHub.get(n.uid()))) {
                fan.add(n);
            } else if (GraphRelaxer.yOf(ys, n.uid()) < hubY) {
                prefix.add(n);
            } else {
                suffix.add(n);
            }
        }
        // 扇窗像素高 = Σ成员高 + 边距×(k−1)；以 hub 中心居中
        double window = -GraphLayout.NODE_MARGIN;
        for (NodeInstance n : fan) {
            window += GraphRelaxer.heightOf(heights, n.uid()) + GraphLayout.NODE_MARGIN;
        }
        double startY = hubY + GraphRelaxer.heightOf(heights, hub) / 2 - window / 2;
        double y = startY - GraphLayout.NODE_MARGIN;
        for (int i = prefix.size() - 1; i >= 0; i--) {
            y -= GraphRelaxer.heightOf(heights, prefix.get(i).uid());
            ys.put(prefix.get(i).uid(), y);
            y -= GraphLayout.NODE_MARGIN;
        }
        y = startY;
        for (NodeInstance n : fan) {
            ys.put(n.uid(), y);
            y += GraphRelaxer.heightOf(heights, n.uid()) + GraphLayout.NODE_MARGIN;
        }
        for (NodeInstance n : suffix) {
            ys.put(n.uid(), y);
            y += GraphRelaxer.heightOf(heights, n.uid()) + GraphLayout.NODE_MARGIN;
        }
        group.clear();
        group.addAll(prefix);
        group.addAll(fan);
        group.addAll(suffix);
    }
}
