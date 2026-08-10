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
import java.util.TreeSet;
/**
 * 分层自动布局（规格 nodegraph-workbench §W2）：<b>到汇最长路径定 x 层（exec 顺序边
 * 0 代价——语句链纵向共列）；层内以 DFS 先序定序（语句链按链序）、距离松弛 + 欲望
 * 重排定 y</b>。
 *
 * <p>坐标遵循编辑器既有约定（versions/1.20.1/run/config/eyelib/nodegraph/ng_smoke.json）：
 * 数据流方向 x 左→右（生产者左、消费者右，汇——装配根——在最右列），列距 300；
 * 垂直间距按节点估计高度逐对计算（{@code 上节点高 + 边距}，默认行距 110 = 标准高 62 + 48）。
 * 无连线的孤立节点（如声明表 ref.*）放在汇左一列。
 *
 * <p>算法阶段（都是确定性纯函数）：
 * <ol>
 *   <li><b>分层 = 到汇最长路径；exec 顺序边 0 代价</b>。exec_out→exec_in 是「同列顺序」
 *       边：语句序列是脚本的纵向自然形态，逐语句独占一列会把 initialize/pre_animation
 *       拉成每列 1-2 节点的水平细针（悦灵 65 语句 ≈ 19500px 宽实证 2026-08-10）。
 *       链成员与链尾同层共列、列内按执行顺序自上而下；槽口边（→body/initialize）
 *       代价仍为 1（链尾在槽主左一列）。</li>
 *   <li><b>层内定序 = 汇出发反向 DFS 先序</b>。表达式链在上游几乎全是独占树（唯一消费者），
 *       DFS 先序天然让链/子树成员在层内连续——实测（悦灵 812 节点，2026-08-07）比重心排序
 *       极端边数 29→9：重心排序每趟重置均布 y 会破坏链连续性（曾引以为傲的「簇邻接」收益
 *       远小于链断裂损失）。</li>
 *   <li><b>巨扇分列</b>（{@link #splitGiantFans}）：「唯一消费者同为某 hub」或「唯一生产者
 *       同为某 hub」的成员群超过单列上限时切成多列（不限度数，带表达式链的 rc.root 也算）。
 *       分列只缩冠幅占用的列高、不强行移动成员——成员位置由链锚定，强挪会把长边挪进链里
 *       （实测 Σ +48%）。</li>
 *   <li><b>邻居中位数播种</b>：y 不由均布起步，而是从汇侧列向源侧列逐列取「已播种邻居
 *       的中位数」播种（单趟）。均布起步会让压紧栈陷入局部均衡——栈底被低欲望节点钉住、
 *       整栈无法协同下沉到链所在的盆域（悦灵 rc.root 栈实证：均布起步收敛后 max=6712，
 *       播种后 Σ −31%、max=4093）。多趟播种反而让上游影响回流拖坏（Σ +4%）。</li>
 *   <li><b>距离松弛</b>：高斯-塞德尔迭代，每轮先把每个节点向「全部邻居的中位数 y」拉拢
 *       （L1 目标的单点最优；clamp 保持层内顺序与节点高度间距），再做<b>纯叶扇居中</b>
 *       （度数为 1 且共享唯一 hub 的层内连续段作为刚性整体平移到 hub y——压紧段对逐节点
 *       趟无自由度，单向堆叠就靠这个拆开）。轮数按图规模给足并带早退。</li>
 *   <li><b>列内欲望重排</b>（{@link #reorderColumnsByDesire}，两趟）：clamp 保序会把
 *       「节点与其邻居顺序不对应」的垂直错位锁死（悦灵语句值链想要 y≈2500 被钉在
 *       y≈9600 实证）。每列<b>非语句链</b>节点保持槽位集合不变、按邻居中位数欲望 y
 *       重新分配槽位后再松弛（语句链顺序是语义，不参与）。</li>
 *   <li><b>扇形居中重排</b>（{@link #improveLeafFans}，实测验收的局部搜索）：对明显偏离
 *       理论最优的纯叶扇执行「按 hub 侧分区重排 + 扇窗居中压紧」，完整松弛后以全图 Σ|Δy|
 *       实测判定去留——单调下降，无振荡。</li>
 *   <li><b>hub 极小极大定位</b>（{@link #centerHubsOnFanSpan}）：把大扇 hub 移到扇成员 y 跨
 *       中心（clamp 不撞列内邻居）。松弛的中位数投票把 hub 停在 L1 最优点（偏向成员密集侧），
 *       扇尾成员的边距可达扇跨全长；移到跨中把扇的最大边压到跨度一半，牺牲的 Σ 很小
 *       （hub 移动只影响其自身边）。扇跨度本身由成员的链锚定，不可压缩。</li>
 *   <li><b>声明通道最后共位</b>：声明边（var-ref read:/write:、variable.in 写入、
 *       ref→root 声明端口）<b>不进流邻接</b>——分层/播种/松弛/扇机器全部只看流边。
 *       但 <b>animate.entry 与有 entry 消费者的 ref.ac/ref.animation 是流节点</b>
 *       （2026-08-10 重设计）：entry→root animate 边入流定层 1，ref→entry 边入流贴
 *       entry 高度——簇内堆叠曾把 ref.ac 钉在 entry 上方 dy≈2094、把 weight 链尾钉到
 *       entry 右列造成回流边 avgMan≈3074（悦灵实证）。纯声明 ref（sound/particle/
 *       无 entry 消费者）按<b>方形块</b>打包到 root 左侧子列（列数 = √(总高/列距)）；
 *       declvar/纯写入变量按<b>逐成员锚点 y</b> 共位到锚点侧子列（切块中位数居中曾把
 *       69 写变量摊成 7 列、writer 边 dx 拉到 900-2700）。</li>
 * </ol>
 * 扇居中平移可能产生负 y，出口处统一平移归一化（全图最小 y = 0，不改变任何边长）。
 * 环只做防御（层按 0 计），环本身由 GraphValidator.CYCLE 报告。
 *
 * <p><b>已证伪的改进方向（悦灵实测，仿真与实机一致）</b>——勿重试：
 * ① 巨扇成员强制居中（整列居中/实测验收居中/分列后块级居中/一次性居中后再松弛）：
 * 链跟不上，长边从扇边挪进链边（Σ +48%、+12%、+9% 不等）；且整列居中与逐节点松弛
 * 恒幅振荡，不能进早退循环；
 * ② 远邻 blend（median 向 >2000 的邻居偏移）：Σ 与极端边数齐升；
 * ③ 松弛后再重心排序交替：收敛到同一盆域，无收益（注意：{@link #reorderColumnsByDesire}
 * 是槽位重排 + 再松弛的单/两趟定向修复，与交替迭代不同，勿混淆）；
 * ④ 分列前按类型聚类（无居中配合时）：Σ +2.4%，纯叶段仍被度数>1 成员打断；
 * ⑤ 播种多趟迭代：上游影响回流，Σ +4%、max +24%。
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
    /** 共位声明芯片列内间距（variable/ref 小节点不需要 48px 呼吸区，紧排防纵漂）。 */
    private static final float CLUSTER_GAP = 10f;
    /** 极小极大定位的最小扇规模：小于此数的扇 hub 交给中位数投票即可。 */
    private static final int MIN_MINIMAX_FAN_SIZE = 4;

    /**
     * 分层布局：返回坐标重排后的节点列表（其余字段直通，顺序与输入一致）。
     *
     * <p>层定义：汇（无出线的节点）为 0 层；其余节点 = 1 + 消费者层最大值（到汇最长路径）。
     * x = (maxLayer − layer) × {@link #X_SPACING}（汇在最右）；y 见类级文档两阶段。
     */
    public static List<NodeInstance> layout(List<NodeInstance> nodes, List<Wire> wires) {
        Map<String, String> typeOf = new HashMap<>();
        for (NodeInstance n : nodes) {
            typeOf.put(n.uid(), n.type());
        }
        Map<String, List<String>> consumers = new HashMap<>();
        Map<String, List<String>> producers = new HashMap<>();
        Map<String, List<Wire>> producerWires = new HashMap<>();
        // 流邻接（v9）：剔除 variable.in 写入通道边——写入不约束左→右分层
        // （v.x = v.x + 1 模式下写入边与读取边互成环，到汇最长路径必须走纯流图）
        Map<String, List<String>> flowConsumers = new HashMap<>();
        Set<String> flowWiredNodes = new HashSet<>();
        // 声明通道边（var-ref read:/write: 或写入通道）：不参与分层——declvar 变量节点
        // 由专门的共位子列贴靠其 ref（见下方 declvarByRefLayer），否则最长路径把它们
        // 钉在「最左消费者的上游」，横跨数列拉出长扇（用户实机截图实证 2026-08-09）
        Map<String, Set<String>> declvarRefs = new HashMap<>();
        Map<String, Set<String>> writeVarWriters = new HashMap<>();
        // declvar 的读/写方向（共位侧选择）：ref.write: 输出 → 变量节点在 ref 右侧；
        // ref.read: 输入 → 左侧（左读右写的空间表达）
        Set<String> declvarWriteVars = new HashSet<>();
        for (Wire w : wires) {
            consumers.computeIfAbsent(w.from().node(), k -> new ArrayList<>()).add(w.to().node());
            producers.computeIfAbsent(w.to().node(), k -> new ArrayList<>()).add(w.from().node());
            producerWires.computeIfAbsent(w.to().node(), k -> new ArrayList<>()).add(w);
            boolean writeInput = io.github.tt432.eyelib.nodegraph.NodeTypes.isVariableWriteInput(
                    typeOf.getOrDefault(w.to().node(), ""), w.to().port());
            boolean varRefEdge = io.github.tt432.eyelib.nodegraph.NodeTypes.isVarRefPort(w.to().port())
                    || io.github.tt432.eyelib.nodegraph.NodeTypes.isVarRefPort(w.from().port());
            if (varRefEdge) {
                String varUid = "variable".equals(typeOf.getOrDefault(w.from().node(), ""))
                        ? w.from().node() : w.to().node();
                String refUid = varUid.equals(w.from().node()) ? w.to().node() : w.from().node();
                declvarRefs.computeIfAbsent(varUid, k -> new TreeSet<>()).add(refUid);
                if (w.from().port().startsWith("write:")) {
                    declvarWriteVars.add(varUid);
                }
            } else if (writeInput) {
                // set_var.target → variable.in（非 var-ref 的纯写入边）
                writeVarWriters.computeIfAbsent(w.to().node(), k -> new TreeSet<>()).add(w.from().node());
            }
        }
        // 纯声明/写入变量节点（全部边都是声明通道）：不进流分层，稍后共位插入——
        // 纯度按全量邻接直判（declvar=全部线都是 var-ref 边；writeOnly=全部线都是写入边）
        Map<String, String> declvarReadOf = new HashMap<>(); // varUid -> 主 ref uid（字典序最小，确定性）
        Map<String, String> declvarWriteOf = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : declvarRefs.entrySet()) {
            boolean pure = true;
            for (Wire w : wires) {
                boolean touches = w.from().node().equals(e.getKey()) || w.to().node().equals(e.getKey());
                if (touches && !io.github.tt432.eyelib.nodegraph.NodeTypes.isVarRefPort(w.from().port())
                        && !io.github.tt432.eyelib.nodegraph.NodeTypes.isVarRefPort(w.to().port())) {
                    pure = false;
                    break;
                }
            }
            if (pure) {
                (declvarWriteVars.contains(e.getKey()) ? declvarWriteOf : declvarReadOf)
                        .put(e.getKey(), e.getValue().iterator().next());
            }
        }
        Map<String, String> writeOnlyOf = new HashMap<>(); // varUid -> 主写入方 uid
        for (Map.Entry<String, Set<String>> e : writeVarWriters.entrySet()) {
            if (declvarRefs.containsKey(e.getKey())) {
                continue;
            }
            boolean pure = true;
            for (Wire w : wires) {
                boolean touches = w.from().node().equals(e.getKey()) || w.to().node().equals(e.getKey());
                if (touches && !io.github.tt432.eyelib.nodegraph.NodeTypes.isVariableWriteInput(
                        typeOf.getOrDefault(w.to().node(), ""), w.to().port())) {
                    pure = false;
                    break;
                }
            }
            if (pure) {
                writeOnlyOf.put(e.getKey(), e.getValue().iterator().next());
            }
        }
        // root 声明簇共位：ref.* / animate.entry 与 root 之间是纯声明通道，最长路径把它们
        // 留在源侧、向 root 拉出横跨全图的扇（悦灵 65 条 ref.sound 边实证）。整簇按
        // 视觉序 [读 declvar][ref][写 declvar][root] 贴到 root 左侧子列。
        // ref 纯度：无非 var-ref 的上游。**有 entry 消费者的 ref（ref.ac/ref.animation
        // →animate.entry:ref）不进簇**——留在流分层，ref→entry 是流边，松弛把 ref 贴到
        // entry 的 y（簇内从列顶堆叠曾把 ref.ac 堆在 entry 上方 dy≈2094，悦灵实证
        // 2026-08-10）；animate.entry 本身同样是流节点（animate→root 入流定层 1，
        // condition/weight 链入流——链尾曾因 entry 脱离流邻接沦为汇、被钉到 root 右列
        // 造成回流边 avgMan≈3074）。
        Set<String> rootTypes = Set.of("entity.root", "rc.root");
        Set<String> rootDeclPorts = Set.of("animations", "animation_controllers", "particles", "sounds");
        Map<String, String> rootDeclOf = new HashMap<>(); // refUid -> root uid
        {
            Set<String> declRefTypes = Set.of("ref.sound", "ref.particle", "ref.ac", "ref.animation");
            for (NodeInstance n : nodes) {
                if (!declRefTypes.contains(n.type())) {
                    continue;
                }
                String anchor = null;
                boolean pure = true;
                boolean entryConnected = false;
                for (Wire w : wires) {
                    boolean out = w.from().node().equals(n.uid());
                    boolean in = w.to().node().equals(n.uid());
                    if (!out && !in) {
                        continue;
                    }
                    if (io.github.tt432.eyelib.nodegraph.NodeTypes.isVarRefPort(w.from().port())
                            || io.github.tt432.eyelib.nodegraph.NodeTypes.isVarRefPort(w.to().port())) {
                        continue;
                    }
                    if (in) {
                        pure = false;
                        break;
                    }
                    if (rootDeclPorts.contains(w.to().port())
                            && rootTypes.contains(typeOf.getOrDefault(w.to().node(), ""))) {
                        anchor = w.to().node();
                    }
                    if ("ref".equals(w.to().port())
                            && "animate.entry".equals(typeOf.getOrDefault(w.to().node(), ""))) {
                        entryConnected = true;
                    }
                }
                if (pure && anchor != null && !entryConnected) {
                    rootDeclOf.put(n.uid(), anchor);
                }
            }
        }
        // 第二遍：纯流邻接——剔除声明通道边与「触及簇成员的边」（簇成员由簇放置定位，
        // 其边若入流会把簇邻居的期望拖向未放置的簇成员@0）
        Set<String> clusterMembers = new HashSet<>();
        clusterMembers.addAll(rootDeclOf.keySet());
        clusterMembers.addAll(declvarReadOf.keySet());
        clusterMembers.addAll(declvarWriteOf.keySet());
        clusterMembers.addAll(writeOnlyOf.keySet());
        Map<String, List<String>> flowProducers = new HashMap<>();
        List<Wire> flowWires = new ArrayList<>();
        for (Wire w : wires) {
            if (io.github.tt432.eyelib.nodegraph.NodeTypes.isVariableWriteInput(
                    typeOf.getOrDefault(w.to().node(), ""), w.to().port())
                    || io.github.tt432.eyelib.nodegraph.NodeTypes.isVarRefPort(w.to().port())
                    || io.github.tt432.eyelib.nodegraph.NodeTypes.isVarRefPort(w.from().port())) {
                continue;
            }
            // root 声明表边（ref→root 的 animations/animation_controllers/particles/sounds）
            // 是声明通道：不入流——声明扇不污染 root 的中位数投票；entry-connected ref 的
            // 分层由 ref→entry 流边承担（entry→root 的 animate 边不在此列，保留入流）
            if (rootDeclPorts.contains(w.to().port())
                    && rootTypes.contains(typeOf.getOrDefault(w.to().node(), ""))) {
                continue;
            }
            if (clusterMembers.contains(w.from().node()) || clusterMembers.contains(w.to().node())) {
                continue;
            }
            flowConsumers.computeIfAbsent(w.from().node(), k -> new ArrayList<>()).add(w.to().node());
            flowProducers.computeIfAbsent(w.to().node(), k -> new ArrayList<>()).add(w.from().node());
            flowWiredNodes.add(w.from().node());
            flowWiredNodes.add(w.to().node());
            flowWires.add(w);
        }
        consumers.values().forEach(list -> list.sort(Comparator.naturalOrder()));
        producers.values().forEach(list -> list.sort(Comparator.naturalOrder()));
        flowConsumers.values().forEach(list -> list.sort(Comparator.naturalOrder()));
        producerWires.values().forEach(list -> list.sort(
                Comparator.comparing((Wire w) -> w.from().node()).thenComparing(w -> w.from().port())));

        // exec 语句链：exec_out → exec_in 是「同列顺序」边——语句序列是脚本的自然
        // 纵向形态，逐语句独占一列会把 initialize/pre_animation 拉成每列 1-2 节点的
        // 水平细针（悦灵 65 条语句 × 300 ≈ 19500px 宽实证 2026-08-10）。分层时顺序边
        // 代价为 0（链成员与链尾同层），槽口边（exec_out → body/initialize 等非 exec_in
        // 端口）代价仍为 1（链尾在槽主左一列）；值边不变。
        Set<String> seqPairs = new HashSet<>();      // "from\0to"：顺序边（0 代价）
        Map<String, String> seqNext = new HashMap<>(); // 链遍历：exec_out → exec_in 唯一后继
        Map<String, String> seqPrev = new HashMap<>();
        for (Wire w : wires) {
            if (!"exec_out".equals(w.from().port()) || !"exec_in".equals(w.to().port())) {
                continue;
            }
            seqPairs.add(w.from().node() + "\0" + w.to().node());
            seqNext.merge(w.from().node(), w.to().node(), (a, b) -> a.compareTo(b) <= 0 ? a : b);
            seqPrev.merge(w.to().node(), w.from().node(), (a, b) -> a.compareTo(b) <= 0 ? a : b);
        }

        // x 层：到汇最长路径（记忆化 DFS；环防御 → 0；exec 顺序边 0 代价——语句链共列）
        Map<String, Integer> layers = new HashMap<>();
        int maxLayerValue = 0;
        for (NodeInstance node : nodes) {
            if (flowWiredNodes.contains(node.uid())) {
                int layer = layerOf(node.uid(), flowConsumers, seqPairs, layers, new HashSet<>());
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
            if (!flowConsumers.containsKey(node.uid())) {
                sinks.add(node.uid());
            }
        }
        sinks.sort(Comparator.naturalOrder());
        Set<String> visited = new HashSet<>();
        int[] seq = {0};
        for (String sink : sinks) {
            dfsOrder(sink, producerWires, visited, order, seq);
        }

        // 语句链显式定序：链内成员排序键 = (链尾的 DFS 序, 链内序号)——DFS 先序沿
        // producerWires 会按 uid 交错值链节点，不保证语句按执行顺序相邻；显式键让
        // 链在共位列内按 exec 顺序自上而下、整条链占据连续行段（多链按链尾 DFS 序相接）。
        Map<String, Integer> stmtPrimary = new HashMap<>();
        Map<String, Integer> stmtIndex = new HashMap<>();
        {
            Set<String> chainNodes = new TreeSet<>();
            chainNodes.addAll(seqNext.keySet());
            chainNodes.addAll(seqPrev.keySet());
            Set<String> chained = new HashSet<>();
            for (String head : chainNodes) {
                if (seqPrev.containsKey(head) || !chained.add(head)) {
                    continue;
                }
                List<String> chain = new ArrayList<>();
                String cur = head;
                Set<String> guard = new HashSet<>();
                while (cur != null && guard.add(cur)) {
                    chain.add(cur);
                    chained.add(cur);
                    cur = seqNext.get(cur);
                }
                int primary = order.getOrDefault(chain.get(chain.size() - 1), Integer.MAX_VALUE);
                for (int i = 0; i < chain.size(); i++) {
                    stmtPrimary.put(chain.get(i), primary);
                    stmtIndex.put(chain.get(i), i);
                }
            }
        }

        // 同层分组（层号升序 = 汇→源，确定性遍历），按 DFS 先序排定层内顺序
        // （语句链成员按显式链序）；纯声明/写入变量节点不进普通列（稍候插入共位子列）
        Map<ColumnKey, List<NodeInstance>> byLayer = new TreeMap<>();
        Map<String, Integer> layerOfUid = new HashMap<>();
        for (NodeInstance node : nodes) {
            int layer = flowWiredNodes.contains(node.uid()) ? layers.get(node.uid()) : isolatedLayer;
            if (declvarReadOf.containsKey(node.uid()) || declvarWriteOf.containsKey(node.uid())
                    || writeOnlyOf.containsKey(node.uid()) || rootDeclOf.containsKey(node.uid())) {
                continue;
            }
            layerOfUid.put(node.uid(), layer);
            byLayer.computeIfAbsent(new ColumnKey(layer, 0), k -> new ArrayList<>()).add(node);
        }
        byLayer.values().forEach(group -> group.sort(Comparator
                .comparingInt((NodeInstance n) -> stmtPrimary.getOrDefault(n.uid(),
                        order.getOrDefault(n.uid(), Integer.MAX_VALUE)))
                .thenComparingInt(n -> stmtIndex.getOrDefault(n.uid(), -1))
                .thenComparing(NodeInstance::uid)));

        // 纯叶（度数=1）→ 唯一邻居；唯一消费者/唯一生产者（去重后计，同两节点间多根线
        // 会产生重复项，必须 distinct）。全部只按**流边**计——声明扇（65×ref.sound→root）
        // 不参与扇机器（分列/极小极大），其放置由簇打包全权负责
        Map<String, String> soleHub = new HashMap<>();
        Map<String, Integer> degree = new HashMap<>();
        for (Wire w : flowWires) {
            degree.merge(w.from().node(), 1, Integer::sum);
            degree.merge(w.to().node(), 1, Integer::sum);
            soleHub.put(w.from().node(), w.to().node());
            soleHub.put(w.to().node(), w.from().node());
        }
        soleHub.entrySet().removeIf(e -> degree.getOrDefault(e.getKey(), 0) != 1);
        Map<String, String> soleConsumer = new HashMap<>();
        Map<String, String> soleProducer = new HashMap<>();
        for (Map.Entry<String, List<String>> e : flowConsumers.entrySet()) {
            List<String> distinct = e.getValue().stream().distinct().toList();
            if (distinct.size() == 1) {
                soleConsumer.put(e.getKey(), distinct.get(0));
            }
        }
        for (Map.Entry<String, List<String>> e : flowProducers.entrySet()) {
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

        // 初始 y：先按累计高度均布兜底，再从汇侧列向源侧列做一次邻居中位数播种
        // （Sugiyama 式初始化）。均布从 0 起的播种会让压紧栈陷入局部均衡：栈底被
        // 低欲望节点钉住，整栈无法协同下沉到链所在的全局盆域（悦灵 rc.root 栈实证，
        // 松弛 24 轮收敛在 max=6712 的陷阱里）。
        Map<String, Double> ys = new HashMap<>();
        byLayer.values().forEach(group -> {
            double y = 0;
            for (NodeInstance n : group) {
                ys.put(n.uid(), y);
                y += heightOf(heights, n.uid()) + NODE_MARGIN;
            }
        });
        {
            // byLayer 是 TreeMap（层号升序 = 汇→源），层内保持 DFS 序播种；clamp 保持
            // 层内顺序与最小间距。每个节点取「已播种邻居」的中位数，无则留均布位。
            // 单趟足够：两趟会让上游影响回流把图向下游拖（实测 Σ +4%、max +24%）。
            for (List<NodeInstance> group : byLayer.values()) {
                double prevBottom = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < group.size(); i++) {
                    NodeInstance n = group.get(i);
                    double current = yOf(ys, n.uid());
                    double desired = medianAllNeighborsY(n.uid(), flowConsumers, flowProducers, ys);
                    double seeded = Double.isNaN(desired) ? current
                            : Math.max(desired, prevBottom + NODE_MARGIN);
                    ys.put(n.uid(), seeded);
                    prevBottom = seeded + heightOf(heights, n.uid());
                }
            }
        }

        // 共位子列（此时全部普通列已完成播种，锚点 y 可读）：
        // - root 声明簇（ref + entry + 其 declvar）→ 协调放置到 root 左侧连续子列
        // - 其余 declvar 读边 → 主 ref 左侧；写边 / 纯写入变量 → 锚点右侧
        Map<String, NodeInstance> byUid = new HashMap<>();
        for (NodeInstance n : nodes) {
            byUid.put(n.uid(), n);
        }
        Map<String, String> genericReadOf = new HashMap<>();
        Map<String, String> genericWriteOf = new HashMap<>();
        Map<String, String> clusterReadOf = new HashMap<>();
        Map<String, String> clusterWriteOf = new HashMap<>();
        for (Map.Entry<String, String> e : declvarReadOf.entrySet()) {
            (rootDeclOf.containsKey(e.getValue()) ? clusterReadOf : genericReadOf)
                    .put(e.getKey(), e.getValue());
        }
        for (Map.Entry<String, String> e : declvarWriteOf.entrySet()) {
            (rootDeclOf.containsKey(e.getValue()) ? clusterWriteOf : genericWriteOf)
                    .put(e.getKey(), e.getValue());
        }
        relaxDistances(byLayer, flowConsumers, flowProducers, ys, soleHub, heights, nodes.size());
        // 列内按欲望重排：列内顺序在松弛前由 DFS/链序定死，clamp 保序意味着「节点与其
        // 邻居顺序不对应」的垂直错位被锁死（悦灵 x=1800 列实证：语句值链想要 y≈2500
        // 被钉在 y≈9600，dy≈7000）。把每列**非语句链**节点占据的 y 槽位按「邻居中位数
        // 欲望」重排后再松弛——语句链（执行序是语义）与槽位骨架不动。
        reorderColumnsByDesire(byLayer, flowConsumers, flowProducers, ys, seqNext, seqPrev);
        relaxDistances(byLayer, flowConsumers, flowProducers, ys, soleHub, heights, nodes.size());
        // 第二趟：松弛后的 y 让欲望估值更准（首趟重排基于未收敛 y）
        reorderColumnsByDesire(byLayer, flowConsumers, flowProducers, ys, seqNext, seqPrev);
        relaxDistances(byLayer, flowConsumers, flowProducers, ys, soleHub, heights, nodes.size());
        improveLeafFans(byLayer, soleHub, flowConsumers, flowProducers, flowWires, ys, heights, nodes.size());
        centerHubsOnFanSpan(byLayer, soleConsumer, soleProducer, ys, flowWires, heights);

        // 声明共位放置（最后做：锚点 y 已收敛，簇/共位列直接按终态锚点居中；
        // 提前做会被「压紧列无松弛自由度」钉在种子的位——悦灵 ref.ac 列实证 dy≈3400）
        if (!rootDeclOf.isEmpty()) {
            placeDeclCluster(byLayer, ys, consumers, producers, heights,
                    layerOfUid, isolatedLayer, byUid,
                    rootDeclOf, clusterReadOf, clusterWriteOf);
        }
        if (!genericReadOf.isEmpty()) {
            insertCoLocatedColumns(byLayer, ys, consumers, producers, heights,
                genericReadOf, layerOfUid, isolatedLayer, byUid, -1);
        }
        if (!genericWriteOf.isEmpty()) {
            insertCoLocatedColumns(byLayer, ys, consumers, producers, heights,
                genericWriteOf, layerOfUid, isolatedLayer, byUid, 1);
        }
        if (!writeOnlyOf.isEmpty()) {
            insertCoLocatedColumns(byLayer, ys, consumers, producers, heights,
                writeOnlyOf, layerOfUid, isolatedLayer, byUid, 1);
        }
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

    /** 列键：layer = 到汇最长路径层；sub = 巨扇分列的子列偏移（0=主列，负=左，正=右）。 */    private record ColumnKey(int layer, int sub) implements Comparable<ColumnKey> {
        @Override
        public int compareTo(ColumnKey other) {
            int c = Integer.compare(layer, other.layer);
            return c != 0 ? c : Integer.compare(sub, other.sub);
        }
    }

    /**
     * 共位子列插入：把声明/写入变量节点按主锚点（ref 或 set_var）所在层分组成子列，
     * 放在该层 direction<0（最左子列再左一格）/ direction>0（最右子列再右一格）侧；
     * y 取全部锚点的已播种 y 中位数，同子列按锚点 y 排序后顺序 clamp 防重叠。
     * 调用时机：普通列完成播种之后、松弛之前（锚点 y 已可读，松弛继续微调）。
     */
    private static void insertCoLocatedColumns(
            Map<ColumnKey, List<NodeInstance>> byLayer,
            Map<String, Double> ys,
            Map<String, List<String>> consumers,
            Map<String, List<String>> producers,
            Map<String, Float> heights,
            Map<String, String> anchorOf,
            Map<String, Integer> layerOfUid, int isolatedLayer,
            Map<String, NodeInstance> byUid, int direction) {
        if (anchorOf.isEmpty()) {
            return;
        }
        Map<Integer, List<String>> byAnchorLayer = new TreeMap<>();
        for (Map.Entry<String, String> e : anchorOf.entrySet()) {
            int layer = layerOfUid.getOrDefault(e.getValue(), isolatedLayer);
            byAnchorLayer.computeIfAbsent(layer, k -> new ArrayList<>()).add(e.getKey());
        }
        for (Map.Entry<Integer, List<String>> e : byAnchorLayer.entrySet()) {
            int edge = 0;
            for (ColumnKey key : byLayer.keySet()) {
                if (key.layer() == e.getKey()) {
                    edge = direction < 0 ? Math.min(edge, key.sub()) : Math.max(edge, key.sub());
                }
            }
            List<List<NodeInstance>> columns = packColocatedChunks(e.getValue(), anchorOf,
                    ys, consumers, producers, heights, byUid, false);
            for (int i = 0; i < columns.size(); i++) {
                int sub = direction < 0 ? edge - 1 - i : edge + 1 + i;
                byLayer.put(new ColumnKey(e.getKey(), sub), columns.get(i));
            }
        }
    }

    /**
     * 共位组打包：按锚点 y 排序（uid 兜底）后**逐成员贴自己锚点的 y** 顺序 clamp 堆叠——
     * 「切块 + 块内锚点中位数居中」会让尾部成员远离锚点（悦灵 45 变量 ref 实证 dy±1700），
     * 按 10 人切多列又把写变量摊成 7 个并列子列、writer 边 dx 拉到 900-2700（悦灵实证
     * 2026-08-10）。锚点 y 有序时堆叠顺序 = 锚点顺序，每成员 dy 只剩堆叠漂移
     * （锚点比堆叠更密时才有，实测很小）。
     * anchorOnly=true 时欲望值只取锚点 y（声明簇成员的其他邻居此时还未播种，
     * 中位数会被默认值污染）。返回单列（调用方决定子列槽位）。
     */
    private static List<List<NodeInstance>> packColocatedChunks(
            List<String> group, Map<String, String> anchorOf,
            Map<String, Double> ys,
            Map<String, List<String>> consumers, Map<String, List<String>> producers,
            Map<String, Float> heights, Map<String, NodeInstance> byUid, boolean anchorOnly) {
        group.sort(Comparator
                .comparingDouble((String uid) -> yOf(ys, java.util.Objects.requireNonNull(
                        anchorOf.get(uid), "anchorOf 键集成员")))
                .thenComparing(uid -> uid));
        List<NodeInstance> column = new ArrayList<>();
        double prevBottom = Double.NEGATIVE_INFINITY;
        for (String uid : group) {
            double d = anchorOnly ? Double.NaN : medianAllNeighborsY(uid, consumers, producers, ys);
            if (Double.isNaN(d)) {
                d = yOf(ys, java.util.Objects.requireNonNull(
                        anchorOf.get(uid), "anchorOf 键集成员"));
            }
            double yy = Math.max(d, prevBottom + NODE_MARGIN);
            ys.put(uid, yy);
            prevBottom = yy + heightOf(heights, uid);
            column.add(byUid.get(uid));
        }
        return List.of(column);
    }

    /**
     * root 声明簇协调放置：把一个 root（entity.root/rc.root）的全部声明成员摆到其左侧
     * 连续子列。ref 与其 declvar **同列混排**（读 declvar 在上、ref 居中、写 declvar 在下）
     * ——同列 dx=0 且垂直相邻，边最短。整簇按**方形块**打包：列数 = √(总高/列距)，
     * 块宽与块高同阶——固定列高上限会把 65 个 ref.sound 堆成 3800px 巨柱拉出 ±1700
     * 竖向扇，按 root 高度切列又会爆出 17 列把簇顶出 30 列开外（dx=9000），两个方向
     * 都有用户实机截图实证（2026-08-09/10）。簇成员只剩纯声明 ref（无 entry 消费者）；
     * 有 entry 消费者的 ref 与 animate.entry 本身都在流分层内（见 layout 的簇判定）。
     */
    private static void placeDeclCluster(
            Map<ColumnKey, List<NodeInstance>> byLayer,
            Map<String, Double> ys,
            Map<String, List<String>> consumers, Map<String, List<String>> producers,
            Map<String, Float> heights,
            Map<String, Integer> layerOfUid, int isolatedLayer,
            Map<String, NodeInstance> byUid,
            Map<String, String> rootDeclOf,
            Map<String, String> clusterReadOf, Map<String, String> clusterWriteOf) {
        // refUid -> 其 declvar（读/写各一表，按变量节点 uid 排序确定）
        Map<String, List<String>> readsOfRef = new HashMap<>();
        Map<String, List<String>> writesOfRef = new HashMap<>();
        for (Map.Entry<String, String> e : clusterReadOf.entrySet()) {
            readsOfRef.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        for (Map.Entry<String, String> e : clusterWriteOf.entrySet()) {
            writesOfRef.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        readsOfRef.values().forEach(list -> list.sort(Comparator.naturalOrder()));
        writesOfRef.values().forEach(list -> list.sort(Comparator.naturalOrder()));

        Set<String> roots = new TreeSet<>();
        roots.addAll(rootDeclOf.values());
        for (String rootUid : roots) {
            int layer = layerOfUid.getOrDefault(rootUid, isolatedLayer);
            int minSub = 0;
            for (ColumnKey key : byLayer.keySet()) {
                if (key.layer() == layer) {
                    minSub = Math.min(minSub, key.sub());
                }
            }
            double rootY = yOf(ys, rootUid);
            // 有序单元序列：纯声明 ref（无 entry 消费者——有 entry 消费者的 ref.ac/
            // ref.animation 已留在流分层贴 entry）。每个 ref 与其 declvar 同列（读上 / ref / 写下）。
            List<String> refs = new ArrayList<>();
            for (Map.Entry<String, String> e : rootDeclOf.entrySet()) {
                if (e.getValue().equals(rootUid)) {
                    refs.add(e.getKey());
                }
            }
            refs.sort(Comparator.naturalOrder());
            List<List<String>> units = new ArrayList<>();
            for (String refUid : refs) {
                List<String> unit = new ArrayList<>();
                unit.addAll(readsOfRef.getOrDefault(refUid, List.of()));
                unit.add(refUid);
                unit.addAll(writesOfRef.getOrDefault(refUid, List.of()));
                units.add(unit);
            }
            // 方形块打包：列数 = √(总高/列距)——固定列高上限只会让块在一个方向上失控
            // （2600 上限时 65 个 sound 堆成 3800px 巨柱拉出 ±1700 竖扇；按 root 高度
            // 切列则爆出 17 列把 ref.animation 顶到 30 列外 dx=9000，用户实机截图实证
            // 2026-08-09）。方形块让 dx 与 dy 同阶：块宽 = cols×列距、块高 = 总高/cols。
            double totalHeight = -CLUSTER_GAP;
            for (List<String> unit : units) {
                for (String uid : unit) {
                    totalHeight += heightOf(heights, uid) + CLUSTER_GAP;
                }
            }
            int cols = units.isEmpty() ? 0 : (int) Math.max(1,
                    Math.round(Math.sqrt(totalHeight / X_SPACING)));
            double columnTarget = cols <= 1 ? Double.MAX_VALUE : totalHeight / cols;
            List<List<String>> columns = new ArrayList<>();
            List<String> current = new ArrayList<>();
            double currentHeight = -CLUSTER_GAP;
            for (List<String> unit : units) {
                double unitHeight = -CLUSTER_GAP;
                for (String uid : unit) {
                    unitHeight += heightOf(heights, uid) + CLUSTER_GAP;
                }
                if (!current.isEmpty() && columns.size() + 1 < cols
                        && currentHeight + CLUSTER_GAP + unitHeight > columnTarget) {
                    columns.add(current);
                    current = new ArrayList<>();
                    currentHeight = -CLUSTER_GAP;
                }
                current.addAll(unit);
                currentHeight += CLUSTER_GAP + unitHeight;
            }
            if (!current.isEmpty()) {
                columns.add(current);
            }
            // 视觉序 [ref 列…][root]；每列以 root.y 居中顺序打包
            int sub = minSub - columns.size();
            for (List<String> column : columns) {
                double columnHeight = -CLUSTER_GAP;
                for (String uid : column) {
                    columnHeight += heightOf(heights, uid) + CLUSTER_GAP;
                }
                double y = rootY - columnHeight / 2;
                List<NodeInstance> placed = new ArrayList<>();
                double prevBottom = Double.NEGATIVE_INFINITY;
                for (String uid : column) {
                    double yy = Math.max(y, prevBottom + CLUSTER_GAP);
                    ys.put(uid, yy);
                    prevBottom = yy + heightOf(heights, uid);
                    placed.add(byUid.get(uid));
                }
                byLayer.put(new ColumnKey(layer, sub++), placed);
            }
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

    /**
     * 列内按欲望重排：每列内**非语句链**节点（不在 exec_out→exec_in 链上）保持其占据的
     * y 槽位集合不变，槽位按升序重新分配给「按邻居中位数欲望 y 排序」的节点（uid 兜底
     * 确定）。语句链成员不动（执行序是语义，不可被布局重排）；重排后需再跑一轮松弛
     * 恢复节点间距。
     */
    private static void reorderColumnsByDesire(Map<ColumnKey, List<NodeInstance>> byLayer,
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
    /** 粒子/音效 ref 节点：预览是 22px 播放键行（非 64px 纹理区），估计取 110。 */
    private static final float REF_PLAY_NODE_HEIGHT = 110f;
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
            case "ref.ac": {
                // 命名变量端口（read:/write:）会显著加高节点——按动态端口数计算，94 兜底
                java.util.Optional<NodeType> dynType = NodeTypes.get(n.type());
                if (dynType.isPresent()) {
                    NodeType t = dynType.get();
                    int rows = Math.max(
                            t.inputsOf(n, NO_SUBGRAPH).size(),
                            t.outputsOf(n, NO_SUBGRAPH).size());
                    return Math.max(REF_NODE_HEIGHT, 28 + 16f * rows + 18f * t.options().size());
                }
                return REF_NODE_HEIGHT;
            }
            case "ref.material":
            case "ref.rc":
                return REF_NODE_HEIGHT;
            case "ref.sound":
            case "ref.particle":
                return REF_PLAY_NODE_HEIGHT;
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
        // v11：call 节点的 args 列表按条目数加行（每行 ≈ 选项行高；列表行含 +-× 控件）
        int optionRows = type.options().size();
        if (isCallType(n.type())) {
            com.google.gson.JsonElement args = n.options().get("args");
            int entries = args != null && args.isJsonArray() ? args.getAsJsonArray().size() : 0;
            optionRows = 1 + entries; // function 行 + 每条目一行（空列表 = 0 行，列表行隐藏）
            if (entries == 0) {
                optionRows = 1;
            }
        }
        return 28 + 16f * rows + 18f * optionRows;
    }

    private static boolean isCallType(String type) {
        return "query.call".equals(type) || "math.call".equals(type) || "exec.call".equals(type);
    }

    private static int layerOf(String uid, Map<String, List<String>> consumers,
                               Set<String> seqPairs,
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
            // exec 顺序边（exec_out→exec_in）0 代价：语句链成员与链尾同层共列
            int cost = seqPairs.contains(uid + "\0" + consumer) ? 0 : 1;
            layer = Math.max(layer, layerOf(consumer, consumers, seqPairs, cache, visiting) + cost);
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
