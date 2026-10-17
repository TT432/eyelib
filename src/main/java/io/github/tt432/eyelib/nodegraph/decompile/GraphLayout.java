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
 *   <li><b>巨扇分列</b>（{@link FanLayoutOptimizer#splitGiantFans}）：「唯一消费者同为某 hub」或「唯一生产者
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
 *   <li><b>列内欲望重排</b>（{@link GraphRelaxer#reorderColumnsByDesire}，两趟）：clamp 保序会把
 *       「节点与其邻居顺序不对应」的垂直错位锁死（悦灵语句值链想要 y≈2500 被钉在
 *       y≈9600 实证）。每列<b>非语句链</b>节点保持槽位集合不变、按邻居中位数欲望 y
 *       重新分配槽位后再松弛（语句链顺序是语义，不参与）。</li>
 *   <li><b>扇形居中重排</b>（{@link FanLayoutOptimizer#improveLeafFans}，实测验收的局部搜索）：对明显偏离
 *       理论最优的纯叶扇执行「按 hub 侧分区重排 + 扇窗居中压紧」，完整松弛后以全图 Σ|Δy|
 *       实测判定去留——单调下降，无振荡。</li>
 *   <li><b>hub 极小极大定位</b>（{@link FanLayoutOptimizer#centerHubsOnFanSpan}）：把大扇 hub 移到扇成员 y 跨
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
 *   <li><b>芯片锚定快照</b>（2026-08-11 重设计，{@link GraphRelaxer#placeSnappedChips}）：variable
 *       芯片不是列公民——列槽位让芯片与消费者恒隔一个列距（写芯片 dy≈1500、读芯片
 *       dx≈2400，用户实机截图指正「关系最近的应在画布上最近」）。全部纯声明/写入变量
 *       与单消费者流变量在列布局收敛后<b>直接贴锚点边侧</b>（左读右写、同锚点芯片块
 *       居中于锚点行），贴靠槽位对的列间隙按需加宽（芯片宽 + 净距超出既有间隙的部分），
 *       芯片整列容纳于间隙、不凸入邻列（凸入实证 82 处重叠）；最后跑仅芯片移动的
 *       重叠消解兜底。流叶变量仍留在列内参与松弛投票（其行槽是链盆域的一部分），
 *       仅终态位置被快照覆盖——实测（悦灵 645 节点）：芯片边 avgDy 114、Σmanhattan
 *       622k（旧 730k，−15%）、Σdy 270k（旧 307k，−12%）、重叠 0、回流 0。</li>
 * </ol>
 * 扇居中平移可能产生负 y，出口处统一平移归一化（全图最小 y = 0，不改变任何边长）。
 * 环只做防御（层按 0 计），环本身由 GraphValidator.CYCLE 报告。
 *
 * <p><b>已证伪的改进方向（悦灵实测，仿真与实机一致）</b>——勿重试：
 * ① 巨扇成员强制居中（整列居中/实测验收居中/分列后块级居中/一次性居中后再松弛）：
 * 链跟不上，长边从扇边挪进链边（Σ +48%、+12%、+9% 不等）；且整列居中与逐节点松弛
 * 恒幅振荡，不能进早退循环；
 * ② 远邻 blend（median 向 >2000 的邻居偏移）：Σ 与极端边数齐升；
 * ③ 松弛后再重心排序交替：收敛到同一盆域，无收益（注意：{@link GraphRelaxer#reorderColumnsByDesire}
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
    static final float NODE_MARGIN = 48f;

    /** 共位声明芯片列内间距（variable/ref 小节点不需要 48px 呼吸区，紧排防纵漂）。 */
    private static final float CLUSTER_GAP = 10f;

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

        // 芯片锚定（2026-08-10 重设计）：variable 芯片不是列公民——列槽位让芯片与消费者
        // 恒隔一个列距（悦灵写芯片 dy≈1500、读芯片 dx≈2400 实机截图实证）。全部纯声明/
        // 写入变量与单消费者流变量不进任何列，布局收敛后直接贴锚点边侧（左读右写、
        // 行向锚点中心堆叠）——关系最近的节点画布距离也最近。
        Map<String, NodeInstance> byUid = new HashMap<>();
        for (NodeInstance n : nodes) {
            byUid.put(n.uid(), n);
        }
        Map<String, String> snapAnchor = new HashMap<>(); // 芯片 → 锚点 uid
        Map<String, Integer> snapSide = new HashMap<>(); // 芯片 → -1 左 / +1 右
        for (Map.Entry<String, String> e : declvarReadOf.entrySet()) {
            snapAnchor.put(e.getKey(), e.getValue());
            snapSide.put(e.getKey(), -1);
        }
        for (Map.Entry<String, String> e : declvarWriteOf.entrySet()) {
            snapAnchor.put(e.getKey(), e.getValue());
            snapSide.put(e.getKey(), 1);
        }
        for (Map.Entry<String, String> e : writeOnlyOf.entrySet()) {
            snapAnchor.put(e.getKey(), e.getValue());
            snapSide.put(e.getKey(), 1);
        }
        // 单消费者流变量（variable.out → 唯一消费者的值边，无生产者）：贴消费者左侧
        for (NodeInstance node : nodes) {
            String uid = node.uid();
            if (!"variable".equals(node.type()) || snapAnchor.containsKey(uid)) {
                continue;
            }
            List<String> flowCons = flowConsumers.getOrDefault(uid, List.of())
                    .stream().distinct().toList();
            if (flowCons.size() == 1 && !flowProducers.containsKey(uid)) {
                snapAnchor.put(uid, flowCons.get(0));
                snapSide.put(uid, -1);
            }
        }
        // root 声明簇的 ref 仍走簇打包（ref 不是芯片）；其 declvar 已被快照接管
        Map<String, String> clusterReadOf = new HashMap<>();
        Map<String, String> clusterWriteOf = new HashMap<>();
        for (Map.Entry<String, String> e : declvarReadOf.entrySet()) {
            if (rootDeclOf.containsKey(e.getValue()) && !snapAnchor.containsKey(e.getKey())) {
                clusterReadOf.put(e.getKey(), e.getValue());
            }
        }
        for (Map.Entry<String, String> e : declvarWriteOf.entrySet()) {
            if (rootDeclOf.containsKey(e.getValue()) && !snapAnchor.containsKey(e.getKey())) {
                clusterWriteOf.put(e.getKey(), e.getValue());
            }
        }
        // 同层分组（层号升序 = 汇→源，确定性遍历），按 DFS 先序排定层内顺序
        // （语句链成员按显式链序）；纯声明/写入变量节点不进普通列（稍候插入共位子列）
        Map<ColumnKey, List<NodeInstance>> byLayer = new TreeMap<>();
        Map<String, Integer> layerOfUid = new HashMap<>();
        for (NodeInstance node : nodes) {
            int layer = flowWiredNodes.contains(node.uid()) ? layers.get(node.uid()) : isolatedLayer;
            // 流叶变量留在列内参与松弛投票（其行槽是链盆域的一部分）；终态位置由快照覆盖
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
        FanLayoutOptimizer.splitGiantFans(byLayer, soleConsumer, soleProducer);

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
                y += GraphRelaxer.heightOf(heights, n.uid()) + NODE_MARGIN;
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
                    double current = GraphRelaxer.yOf(ys, n.uid());
                    double desired = GraphRelaxer.medianAllNeighborsY(
                            n.uid(), flowConsumers, flowProducers, ys);
                    double seeded = Double.isNaN(desired) ? current
                            : Math.max(desired, prevBottom + NODE_MARGIN);
                    ys.put(n.uid(), seeded);
                    prevBottom = seeded + GraphRelaxer.heightOf(heights, n.uid());
                }
            }
        }

        GraphRelaxer.relaxDistances(byLayer, flowConsumers, flowProducers, ys, soleHub, heights, nodes.size());
        // 列内按欲望重排：列内顺序在松弛前由 DFS/链序定死，clamp 保序意味着「节点与其
        // 邻居顺序不对应」的垂直错位被锁死（悦灵 x=1800 列实证：语句值链想要 y≈2500
        // 被钉在 y≈9600，dy≈7000）。把每列**非语句链**节点占据的 y 槽位按「邻居中位数
        // 欲望」重排后再松弛——语句链（执行序是语义）与槽位骨架不动。
        GraphRelaxer.reorderColumnsByDesire(byLayer, flowConsumers, flowProducers, ys, seqNext, seqPrev);
        GraphRelaxer.relaxDistances(byLayer, flowConsumers, flowProducers, ys, soleHub, heights, nodes.size());
        // 第二趟：松弛后的 y 让欲望估值更准（首趟重排基于未收敛 y）
        GraphRelaxer.reorderColumnsByDesire(byLayer, flowConsumers, flowProducers, ys, seqNext, seqPrev);
        GraphRelaxer.relaxDistances(byLayer, flowConsumers, flowProducers, ys, soleHub, heights, nodes.size());
        FanLayoutOptimizer.improveLeafFans(byLayer, soleHub, flowConsumers, flowProducers,
                flowWires, ys, heights, nodes.size());
        FanLayoutOptimizer.centerHubsOnFanSpan(byLayer, soleConsumer, soleProducer, ys, flowWires, heights);

        // 声明共位放置（最后做：锚点 y 已收敛，簇/共位列直接按终态锚点居中；
        // 提前做会被「压紧列无松弛自由度」钉在种子的位——悦灵 ref.ac 列实证 dy≈3400）
        if (!rootDeclOf.isEmpty()) {
            placeDeclCluster(byLayer, ys, consumers, producers, heights,
                    layerOfUid, isolatedLayer, byUid,
                    rootDeclOf, clusterReadOf, clusterWriteOf);
        }
        GraphRelaxer.normalizeY(ys);

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
        // 槽位 x：芯片（宽 120 + 双侧净距 16）整体容纳于列间隙——有芯片贴靠的槽位对
        // 按需加宽（芯片不再凸入邻列，凸入实证 82 处重叠）
        Map<String, Integer> slotOfUid = new HashMap<>();
        for (Map.Entry<String, Integer> e : xIndexOfUid.entrySet()) {
            slotOfUid.put(e.getKey(), e.getValue());
        }
        int slotCount = rightToLeft.size();
        boolean[] slotRightChips = new boolean[slotCount];
        boolean[] slotLeftChips = new boolean[slotCount];
        for (Map.Entry<String, String> e : snapAnchor.entrySet()) {
            Integer slot = slotOfUid.get(e.getValue());
            if (slot == null) {
                continue;
            }
            if (snapSide.getOrDefault(e.getKey(), -1) < 0) {
                slotLeftChips[slot] = true;
            } else {
                slotRightChips[slot] = true;
            }
        }
        // 按需加宽：间隙只需容纳「芯片宽 120 + 净距」超出既有间隙的部分
        // （既有间隙 = 列距 300 − 两侧槽位内容最大宽）
        float[] slotMaxW = new float[slotCount];
        for (Map.Entry<String, Integer> e : slotOfUid.entrySet()) {
            NodeInstance node = byUid.get(e.getKey());
            if (node != null) {
                slotMaxW[e.getValue()] = Math.max(slotMaxW[e.getValue()], estimateWidth(node));
            }
        }
        float[] slotX = new float[slotCount];
        {
            float x = 0;
            for (int i = 0; i < slotCount; i++) {
                if (i > 0 && (slotRightChips[i - 1] || slotLeftChips[i])) {
                    float maxW = Math.max(slotMaxW[i - 1], slotMaxW[i]);
                    x += Math.max(0, maxW + 120 + 2 * GraphRelaxer.CHIP_GAP_X - X_SPACING);
                }
                slotX[i] = x;
                x += X_SPACING;
            }
        }
        Map<String, Float> xsFinal = new HashMap<>();
        for (Map.Entry<String, Integer> e : xIndexOfUid.entrySet()) {
            xsFinal.put(e.getKey(), slotX[e.getValue()]);
        }
        for (NodeInstance n : nodes) {
            xsFinal.putIfAbsent(n.uid(), (maxLayer - isolatedLayer) * X_SPACING);
        }
        // 芯片锚定放置（列布局已收敛，锚点 x/y 终态可读）
        GraphRelaxer.placeSnappedChips(snapAnchor, snapSide, byUid, heights, xsFinal, ys, slotOfUid);
        List<NodeInstance> out = new ArrayList<>(nodes.size());
        for (NodeInstance n : nodes) {
            out.add(new NodeInstance(n.uid(), n.type(),
                    xsFinal.getOrDefault(n.uid(), 0f), ys.getOrDefault(n.uid(), 0.0).floatValue(),
                    n.options(), n.constants()));
        }
        return out;
    }

    /** 列键：layer = 到汇最长路径层；sub = 巨扇分列的子列偏移（0=主列，负=左，正=右）。 */
    record ColumnKey(int layer, int sub) implements Comparable<ColumnKey> {
        @Override
        public int compareTo(ColumnKey other) {
            int c = Integer.compare(layer, other.layer);
            return c != 0 ? c : Integer.compare(sub, other.sub);
        }
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
            double rootY = GraphRelaxer.yOf(ys, rootUid);
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
                    totalHeight += GraphRelaxer.heightOf(heights, uid) + CLUSTER_GAP;
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
                    unitHeight += GraphRelaxer.heightOf(heights, uid) + CLUSTER_GAP;
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
                    columnHeight += GraphRelaxer.heightOf(heights, uid) + CLUSTER_GAP;
                }
                double y = rootY - columnHeight / 2;
                List<NodeInstance> placed = new ArrayList<>();
                double prevBottom = Double.NEGATIVE_INFINITY;
                for (String uid : column) {
                    double yy = Math.max(y, prevBottom + CLUSTER_GAP);
                    ys.put(uid, yy);
                    prevBottom = yy + GraphRelaxer.heightOf(heights, uid);
                    placed.add(byUid.get(uid));
                }
                byLayer.put(new ColumnKey(layer, sub++), placed);
            }
        }
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
     * 节点渲染宽度估计（图坐标单位）：芯片锚定与消解用。类别取典型实测，
     * 未知类型 190（标准节点如 op.binary 约 180，宁高估——高估只多间隙，低估会重叠）。
     */
    public static float estimateWidth(NodeInstance n) {
        return switch (n.type()) {
            case "variable" -> 120f;
            case "const.number", "const.int", "const.bool", "const.color" -> 170f;
            case "const.string" -> 200f;
            case "ref.texture", "ref.geometry" -> 240f;
            case "ref.animation", "ref.ac", "ref.sound", "ref.particle", "ref.material", "ref.rc" -> 230f;
            case "entity.root", "rc.root" -> 220f;
            case "animate.entry", "list.entry", "material.entry", "part_visibility.entry" -> 220f;
            default -> 190f;
        };
    }

    /** 未知类型节点的等效高度（= const.number 实测高 62，配合 {@link #NODE_MARGIN} 保持默认行距 110）。 */
    static final float UNKNOWN_NODE_HEIGHT = 62f;
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
    public static float estimateHeight(NodeInstance n) {
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
