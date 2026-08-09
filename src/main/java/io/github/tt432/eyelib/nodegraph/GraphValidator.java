package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 图验证器（规格 §2/T3）：对 {@link GraphLibrary} / {@link GraphData} 做纯函数式检查，
 * 产出结构化 {@link Diagnostic} 列表。
 *
 * <p>检查项：未知节点类型、uid 重复、wire 端点/方向/类型、多连接与 exec fan-out、
 * 环检测、SLOT 装配白名单、资源引用误用与冲突、根节点计数、子图目标/递归/锚点、
 * 未连接输入、孤儿 exec 链、未声明变量引用、声明类 ref 未接线。
 *
 * <p>验证是纯函数：同输入同输出，不依赖 MC/LDLib；未知类型/端口只产生诊断，不抛异常。
 */
public final class GraphValidator {
    private GraphValidator() {
    }

    /** 子图展开深度上限（规格 §2.4-9）。 */
    public static final int MAX_SUBGRAPH_DEPTH = 32;

    // ---------- 诊断码 ----------
    public static final String UNKNOWN_NODE_TYPE = "UNKNOWN_NODE_TYPE";
    public static final String DUPLICATE_UID = "DUPLICATE_UID";
    public static final String UNKNOWN_WIRE_ENDPOINT = "UNKNOWN_WIRE_ENDPOINT";
    public static final String WIRE_DIRECTION = "WIRE_DIRECTION";
    public static final String TYPE_MISMATCH = "TYPE_MISMATCH";
    public static final String DUPLICATE_INPUT = "DUPLICATE_INPUT";
    public static final String EXEC_FANOUT = "EXEC_FANOUT";
    public static final String CYCLE = "CYCLE";
    public static final String SLOT_KIND = "SLOT_KIND";
    public static final String REF_MISUSE = "REF_MISUSE";
    public static final String ROOT_COUNT = "ROOT_COUNT";
    public static final String SUBGRAPH_TARGET = "SUBGRAPH_TARGET";
    public static final String SUBGRAPH_RECURSION = "SUBGRAPH_RECURSION";
    public static final String SUBGRAPH_ANCHOR = "SUBGRAPH_ANCHOR";
    public static final String REF_CONFLICT = "REF_CONFLICT";
    /** 显式短名非法（非 molang 成员路径）或有效短名为空。 */
    public static final String INVALID_SHORT_NAME = "INVALID_SHORT_NAME";
    public static final String UNCONNECTED_INPUT = "UNCONNECTED_INPUT";
    public static final String ORPHAN_CHAIN = "ORPHAN_CHAIN";
    public static final String UNDECLARED_VARIABLE = "UNDECLARED_VARIABLE";
    /** TEMP 作用域变量同图有读无写（temp 跨求值不存活）。 */
    public static final String TEMP_NEVER_WRITTEN = "TEMP_NEVER_WRITTEN";
    /** 变量声明未选类型（规格 nodegraph-variable-table §2.6）。 */
    public static final String VARIABLE_TYPE_UNKNOWN = "VARIABLE_TYPE_UNKNOWN";
    /** exec.set_var 的 target 未连线到 variable 节点。 */
    public static final String SET_TARGET_NOT_VARIABLE = "SET_TARGET_NOT_VARIABLE";
    /** 声明类 ref 未接线（WARNING，仅 CLIENT_ENTITY 库；规格 inline-render-controller §4）。 */
    public static final String REF_NOT_CONNECTED = "REF_NOT_CONNECTED";
    /** 主图两个内联 rc.root 的 identifier 相同（ERROR，规格 inline-render-controller §4）。 */
    public static final String DUPLICATE_RC_ID = "DUPLICATE_RC_ID";
    /** rc.root 列表端口直连多个条目（v6 链式：只能接链头）。 */
    public static final String LIST_MULTI_HEAD = "LIST_MULTI_HEAD";
    /** 条目 next 链成环。 */
    public static final String LIST_CYCLE = "LIST_CYCLE";
    /** 条目节点不在任何到达 rc.root 的链上（不会出现在输出）。 */
    public static final String ENTRY_ORPHAN = "ENTRY_ORPHAN";

    /** SLOT 装配白名单：目标(节点类型.端口) → 允许的源节点类型。 */
    private static final Map<String, String> SLOT_WHITELIST = Map.ofEntries(
            Map.entry("entity.root.animate", "animate.entry"),
            Map.entry("rc.root.textures", "list.entry"),
            Map.entry("rc.root.materials", "material.entry"),
            Map.entry("rc.root.part_visibility", "part_visibility.entry"),
            Map.entry("ac.root.states", "ac.state"),
            // v10：initial = 初始 state 的图边
            Map.entry("ac.root.initial", "ac.state"),
            Map.entry("ac.state.animations", "animate.entry"),
            Map.entry("ac.state.transitions", "ac.transition"),
            // v10：transition.target 图边 + 粒子条目
            Map.entry("ac.state.incoming", "ac.transition"),
            Map.entry("ac.state.particles", "particle.entry"),
            // v6：条目链（规格 §2.1）——条目的 next 只接同类条目
            Map.entry("list.entry.next", "list.entry"),
            Map.entry("material.entry.next", "material.entry"),
            Map.entry("part_visibility.entry.next", "part_visibility.entry"));

    /** 只能接入装配槽（animate.entry.ref）或 entity.root 动画声明端口的引用节点。 */
    private static final Set<String> ASSEMBLY_ONLY_REFS = Set.of("ref.animation", "ref.ac");
    /** 可接表达式槽、list/material 条目 value 或 RC 锚点声明端口的引用节点。 */
    private static final Set<String> VALUE_REFS = Set.of("ref.geometry", "ref.texture", "ref.material");

    /** 根锚点节点类型 id（可达性分析起点）。 */
    private static final Set<String> ROOT_ANCHORS = Set.of("entity.root", "rc.root", "ac.root", "subgraph.output");

    /** 装配根节点：其输入是「槽」——未连线 = 字段缺省，不报 UNCONNECTED_INPUT。 */
    private static final Set<String> ASSEMBLY_ROOTS = Set.of("entity.root", "rc.root", "ac.root");

    /** 诊断列表是否含 ERROR。 */
    public static boolean hasErrors(List<Diagnostic> diagnostics) {
        return diagnostics.stream().anyMatch(d -> d.severity() == Diagnostic.Severity.ERROR);
    }

    // ====================================================================
    // 库级验证
    // ====================================================================

    /**
     * 验证整个库：逐图检查 + 根节点计数 + 子图目标/递归/锚点 + 跨图资源引用冲突。
     */
    public static List<Diagnostic> validate(GraphLibrary library) {
        List<Diagnostic> out = new ArrayList<>();
        for (Map.Entry<String, GraphData> e : library.graphs().entrySet()) {
            out.addAll(validateGraph(library, e.getKey(), e.getValue()));
        }
        GraphData main = library.graphs().get(library.main());
        if (main != null) {
            checkRootCount(library, main, out);
        } else {
            out.add(Diagnostic.error(ROOT_COUNT, "主图 '" + library.main() + "' 不存在"));
        }
        checkSubgraphAnchors(library, out);
        Map<String, List<CallEdge>> callGraph = checkSubgraphTargets(library, out);
        checkSubgraphRecursion(library, callGraph, out);
        if (main != null) {
            checkRefConflicts(library, callGraph, out);
            checkRefNotConnected(library, out);
            checkDuplicateRcIds(library, out);
            checkEntryChains(library, out);
        }
        return out;
    }

    /** 检查 22（ERROR，仅 CLIENT_ENTITY 库）：主图内联 rc.root 的 identifier 重复。 */
    private static void checkDuplicateRcIds(GraphLibrary library, List<Diagnostic> out) {
        if (library.kind() != GraphKind.CLIENT_ENTITY) {
            return;
        }
        GraphData main = library.graphs().get(library.main());
        if (main == null) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (NodeInstance node : main.nodes()) {
            if (!node.type().equals("rc.root")) {
                continue;
            }
            String id = node.option("identifier", NodeTypes.RC_ROOT).map(JsonElement::getAsString).orElse("");
            if (id.isEmpty()) {
                continue;
            }
            if (!seen.add(id)) {
                out.add(Diagnostic.error(DUPLICATE_RC_ID,
                        "主图存在多个 identifier 为 '" + id + "' 的 rc.root", node.uid()));
            }
        }
    }

    /** v6 三类有序条目类型。 */
    private static final Set<String> CHAIN_ENTRY_TYPES = Set.of(
            "list.entry", "material.entry", "part_visibility.entry");
    /** rc.root 列表端口 → 链头条目类型。 */
    private static final Map<String, String> CHAIN_HEAD_OF_PORT = Map.of(
            "textures", "list.entry",
            "materials", "material.entry",
            "part_visibility", "part_visibility.entry");

    /**
     * 检查 23（ERROR/WARNING，CLIENT_ENTITY 与 RENDER_CONTROLLER 库）：v6 条目链完整性——
     * 列表端口多头（LIST_MULTI_HEAD）、next 链成环（LIST_CYCLE）、条目不在任何到达
     * rc.root 的链上（ENTRY_ORPHAN，含子图条目）。
     */
    private static void checkEntryChains(GraphLibrary library, List<Diagnostic> out) {
        if (library.kind() != GraphKind.CLIENT_ENTITY && library.kind() != GraphKind.RENDER_CONTROLLER) {
            return;
        }
        GraphData main = library.graphs().get(library.main());
        Set<String> claimed = new HashSet<>();
        Set<String> cycleReported = new HashSet<>();
        if (main != null) {
            for (NodeInstance node : main.nodes()) {
                if (!node.type().equals("rc.root")) {
                    continue;
                }
                for (Map.Entry<String, String> port : CHAIN_HEAD_OF_PORT.entrySet()) {
                    List<Wire> direct = main.wires().stream()
                            .filter(w -> w.to().node().equals(node.uid())
                                    && w.to().port().equals(port.getKey()))
                            .toList();
                    if (direct.size() > 1) {
                        out.add(Diagnostic.error(LIST_MULTI_HEAD,
                                "rc.root 的 " + port.getKey() + " 直连了 " + direct.size()
                                        + " 个条目；v6 起列表是链式：只接链头，后续条目挂到前一条目的 next",
                                node.uid()));
                    }
                    for (Wire headWire : direct) {
                        walkEntryChain(main, headWire.from().node(), claimed, cycleReported, out, true);
                    }
                }
            }
        }
        // 孤儿条目：主图未被链认领 + 子图全部条目（到不了 rc.root）
        for (Map.Entry<String, GraphData> e : library.graphs().entrySet()) {
            boolean isMain = e.getKey().equals(library.main());
            for (NodeInstance node : e.getValue().nodes()) {
                if (!CHAIN_ENTRY_TYPES.contains(node.type())) {
                    continue;
                }
                // 脱链的环：从未被认领的条目起走访（只查环，不认领——不到 rc.root 的链无归属）
                if (!claimed.contains(node.uid())) {
                    walkEntryChain(e.getValue(), node.uid(), claimed, cycleReported, out, false);
                }
                if (!isMain || !claimed.contains(node.uid())) {
                    out.add(Diagnostic.warning(ENTRY_ORPHAN,
                            node.type() + " 不在任何到达 rc.root 的条目链上"
                                    + (isMain ? "" : "（位于子图 '" + e.getKey() + "'）") + "，不会出现在输出",
                            node.uid()));
                }
            }
        }
    }

    /**
     * 从条目 uid 沿 next 链走访（next 的连线源 = 后继）；环报 LIST_CYCLE。
     * claim=true（rc.root 链头走访）时认领经过的节点；false 仅查环（脱链表走访不认领）。
     */
    private static void walkEntryChain(GraphData graph, String startUid, Set<String> claimed,
                                       Set<String> cycleReported, List<Diagnostic> out, boolean claim) {
        if (claimed.contains(startUid)) {
            return;
        }
        Set<String> path = new LinkedHashSet<>();
        String cur = startUid;
        while (cur != null) {
            if (!path.add(cur)) {
                if (cycleReported.add(cur)) {
                    out.add(Diagnostic.error(LIST_CYCLE,
                            "条目 next 链成环（经过 " + cur + "）", cur));
                }
                return;
            }
            if (claimed.contains(cur)) {
                break;
            }
            String next = null;
            for (Wire w : graph.wires()) {
                if (w.to().node().equals(cur) && w.to().port().equals("next")) {
                    next = w.from().node();
                    break;
                }
            }
            cur = next;
        }
        if (claim) {
            claimed.addAll(path);
        }
    }

    /** 检查 12：主图根节点恰好 1 个（按库种类）。 */
    private static void checkRootCount(GraphLibrary library, GraphData main, List<Diagnostic> out) {
        String rootType = switch (library.kind()) {
            case CLIENT_ENTITY -> "entity.root";
            case RENDER_CONTROLLER -> "rc.root";
            case ANIMATION_CONTROLLER -> "ac.root";
            case EXPRESSION_LIB -> null;
        };
        if (rootType == null) {
            return;
        }
        long count = main.nodes().stream().filter(n -> n.type().equals(rootType)).count();
        if (count != 1) {
            out.add(Diagnostic.error(ROOT_COUNT,
                    "库种类 " + library.kind().getSerializedName() + " 的主图必须恰好 1 个 " + rootType + "，实际 " + count));
        }
    }

    /** 检查 15：子图锚点数量；主图不得有锚点。 */
    private static void checkSubgraphAnchors(GraphLibrary library, List<Diagnostic> out) {
        for (Map.Entry<String, GraphData> e : library.graphs().entrySet()) {
            String name = e.getKey();
            GraphData graph = e.getValue();
            long inputs = graph.nodes().stream().filter(n -> n.type().equals("subgraph.input")).count();
            long outputs = graph.nodes().stream().filter(n -> n.type().equals("subgraph.output")).count();
            boolean isMain = name.equals(library.main());
            if (isMain) {
                if (inputs > 0 || outputs > 0) {
                    out.add(Diagnostic.error(SUBGRAPH_ANCHOR,
                            "主图不得包含 subgraph.input/subgraph.output 锚点（input=" + inputs + ", output=" + outputs + "）"));
                }
            } else if (graph.isSubgraph()) {
                if (outputs != 1) {
                    out.add(Diagnostic.error(SUBGRAPH_ANCHOR,
                            "子图 '" + name + "' 必须恰好 1 个 subgraph.output，实际 " + outputs));
                }
                if (inputs > 1) {
                    out.add(Diagnostic.error(SUBGRAPH_ANCHOR,
                            "子图 '" + name + "' 至多 1 个 subgraph.input，实际 " + inputs));
                }
            }
        }
    }

    /** 子图调用边（目标图名 + 调用节点 uid）。 */
    private record CallEdge(String target, String nodeUid) {
    }

    /** 检查 13：subgraph.call 指向存在且有接口的子图；同时返回调用图（供递归/引用冲突检查）。 */
    private static Map<String, List<CallEdge>> checkSubgraphTargets(GraphLibrary library, List<Diagnostic> out) {
        Map<String, List<CallEdge>> callGraph = new HashMap<>();
        for (Map.Entry<String, GraphData> e : library.graphs().entrySet()) {
            List<CallEdge> edges = new ArrayList<>();
            for (NodeInstance node : e.getValue().nodes()) {
                if (!node.type().equals("subgraph.call")) {
                    continue;
                }
                String target = node.option("subgraph", NodeTypes.SUBGRAPH_CALL)
                        .map(JsonElement::getAsString).orElse("");
                Optional<GraphData> targetGraph = library.graph(target);
                if (targetGraph.isEmpty() || targetGraph.get().graphInterface().isEmpty()) {
                    out.add(Diagnostic.error(SUBGRAPH_TARGET,
                            "subgraph.call 指向的子图 '" + target + "' 不存在或不是子图（无 interface）", node.uid()));
                } else {
                    edges.add(new CallEdge(target, node.uid()));
                }
            }
            callGraph.put(e.getKey(), edges);
        }
        return callGraph;
    }

    /** 检查 14：子图调用链无环且展开深度 ≤ {@link #MAX_SUBGRAPH_DEPTH}。 */
    private static void checkSubgraphRecursion(GraphLibrary library, Map<String, List<CallEdge>> callGraph,
                                               List<Diagnostic> out) {
        // 环检测（DFS 三色）
        Map<String, Integer> color = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        for (String name : callGraph.keySet()) {
            if (color.getOrDefault(name, 0) == 0) {
                detectCallCycle(name, callGraph, color, stack, out);
            }
        }
        // 深度（memoized 最长链；有环时跳过，环已报错）
        Map<String, Integer> memo = new HashMap<>();
        int depth = callDepth(library.main(), callGraph, memo, new HashSet<>());
        if (depth > MAX_SUBGRAPH_DEPTH) {
            out.add(Diagnostic.error(SUBGRAPH_RECURSION,
                    "子图展开深度 " + depth + " 超过上限 " + MAX_SUBGRAPH_DEPTH));
        }
    }

    private static void detectCallCycle(String start, Map<String, List<CallEdge>> callGraph,
                                        Map<String, Integer> color, Deque<String> stack, List<Diagnostic> out) {
        // 迭代 DFS，避免深链栈溢出
        record Frame(String name, int nextEdge) {
        }
        Deque<Frame> frames = new ArrayDeque<>();
        color.put(start, 1);
        stack.push(start);
        frames.push(new Frame(start, 0));
        while (!frames.isEmpty()) {
            Frame top = frames.peek();
            List<CallEdge> edges = callGraph.getOrDefault(top.name(), List.of());
            if (top.nextEdge() < edges.size()) {
                frames.pop();
                frames.push(new Frame(top.name(), top.nextEdge() + 1));
                CallEdge edge = edges.get(top.nextEdge());
                int c = color.getOrDefault(edge.target(), 0);
                if (c == 1) {
                    out.add(Diagnostic.error(SUBGRAPH_RECURSION,
                            "子图调用链存在环（" + String.join(" → ", stack) + " → " + edge.target() + "）",
                            edge.nodeUid()));
                } else if (c == 0) {
                    color.put(edge.target(), 1);
                    stack.push(edge.target());
                    frames.push(new Frame(edge.target(), 0));
                }
            } else {
                frames.pop();
                color.put(top.name(), 2);
                stack.pop();
            }
        }
    }

    private static int callDepth(String name, Map<String, List<CallEdge>> callGraph,
                                 Map<String, Integer> memo, Set<String> visiting) {
        Integer cached = memo.get(name);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(name)) {
            return 0; // 环：已另行报错
        }
        int depth = 1;
        for (CallEdge edge : callGraph.getOrDefault(name, List.of())) {
            depth = Math.max(depth, 1 + callDepth(edge.target(), callGraph, memo, visiting));
        }
        visiting.remove(name);
        memo.put(name, depth);
        return depth;
    }

    /**
     * 检查 16：主图可达全部图内 ref 的有效短名合法性（INVALID_SHORT_NAME）；
     * REF_CONFLICT 范围 = 主图连到 entity.root 声明端口的 ref（规格 D1：声明 = 连线），
     * 同有效短名不同标识 = 冲突。
     */
    private static void checkRefConflicts(GraphLibrary library, Map<String, List<CallEdge>> callGraph,
                                          List<Diagnostic> out) {
        // BFS 收集可达图
        Set<String> reachable = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        reachable.add(library.main());
        queue.add(library.main());
        while (!queue.isEmpty()) {
            String name = queue.poll();
            for (CallEdge edge : callGraph.getOrDefault(name, List.of())) {
                if (reachable.add(edge.target())) {
                    queue.add(edge.target());
                }
            }
        }
        // 有效短名合法性（全部可达 ref，与是否连线无关）
        for (String graphName : reachable) {
            GraphData graph = library.graphs().get(graphName);
            if (graph == null) {
                continue;
            }
            for (NodeInstance node : graph.nodes()) {
                if (ShortNames.valueOptionOf(node.type()) == null) {
                    continue;
                }
                NodeType type = NodeTypes.require(node.type());
                String explicit = node.option("short_name", type).map(JsonElement::getAsString).orElse("");
                if (!explicit.isEmpty() && ShortNames.isMolangEmitted(node.type())
                        && !ShortNames.isSanitized(explicit)) {
                    out.add(Diagnostic.error(INVALID_SHORT_NAME,
                            "显式短名 '" + explicit + "' 不是合法 molang 成员路径（会发射为 "
                                    + node.type().substring(4) + ".<短名> 表达式）", node.uid()));
                }
                if (ShortNames.effective(node, type).isEmpty()) {
                    out.add(Diagnostic.error(INVALID_SHORT_NAME,
                            node.type() + " 有效短名为空（short_name 与标识符至少填一个）", node.uid()));
                }
            }
        }
        // REF_CONFLICT：仅 CLIENT_ENTITY 主图，声明表集合与组装器同范围（v4：
        // geo/tex/mat = DeclarationTables 的 RC 锚点集合；animations = entity.root 两动画端口）
        if (library.kind() != GraphKind.CLIENT_ENTITY) {
            return;
        }
        GraphData main = library.graphs().get(library.main());
        Optional<NodeInstance> root = main == null ? Optional.empty()
                : main.nodes().stream().filter(n -> n.type().equals("entity.root")).findFirst();
        if (main == null || root.isEmpty()) {
            return;
        }
        // 类别 → 声明集合内的 ref 节点（uid 序）
        Map<String, List<NodeInstance>> declaredByTable = new LinkedHashMap<>(DeclarationTables.collectRefs(main));
        String rootUid = root.get().uid();
        List<NodeInstance> animRefs = new ArrayList<>();
        for (Wire wire : main.wires()) {
            if (wire.to().node().equals(rootUid)
                    && NodeTypes.ENTITY_DECLARATION_PORTS.containsValue(wire.to().port())) {
                main.findNode(wire.from().node()).ifPresent(animRefs::add);
            }
        }
        declaredByTable.put("animations", animRefs);
        // 有效短名 → 标识值，按类别各自一张表
        for (Map.Entry<String, List<NodeInstance>> tableEntry : declaredByTable.entrySet()) {
            Map<String, String> seen = new HashMap<>();
            for (NodeInstance node : tableEntry.getValue()) {
                String valueOption = ShortNames.valueOptionOf(node.type());
                if (valueOption == null) {
                    continue;
                }
                NodeType type = NodeTypes.require(node.type());
                String shortName = ShortNames.effective(node, type);
                if (shortName.isEmpty()) {
                    continue; // 已报 INVALID_SHORT_NAME
                }
                String value = node.option(valueOption, type).map(JsonElement::getAsString).orElse("");
                String prev = seen.putIfAbsent(shortName, value);
                if (prev != null && !prev.equals(value)) {
                    out.add(Diagnostic.error(REF_CONFLICT,
                            "资源引用冲突：" + node.type() + " 短名 '" + shortName + "' 同时指向 '"
                                    + prev + "' 与 '" + value + "'", node.uid()));
                }
            }
        }
    }

    /**
     * 检查 21（WARNING，仅 CLIENT_ENTITY 库；规格 inline-render-controller §4）：声明类 ref 未接线。
     * 主图 ref.{geometry,texture,material} 未接入任何 RC 锚点（rc.root / ref.rc 的声明端口、
     * rc.root 字段端口、条目 value）；ref.animation/ref.ac 未连 entity.root 动画声明端口
     * （仅接 animate.entry 的情形消息单独点明）；ref.rc / rc.root 的 controller 未接
     * entity.root.render_controllers；子图中的声明类 ref 提示移至主图。
     * 标识符选项无实例值的占位 ref 不报（UNKNOWN_REFERENCE 已覆盖）。
     */
    private static void checkRefNotConnected(GraphLibrary library, List<Diagnostic> out) {
        if (library.kind() != GraphKind.CLIENT_ENTITY) {
            return;
        }
        GraphData main = library.graphs().get(library.main());
        Optional<NodeInstance> root = main == null ? Optional.empty()
                : main.nodes().stream().filter(n -> n.type().equals("entity.root")).findFirst();
        if (main == null || root.isEmpty()) {
            return;
        }
        String rootUid = root.get().uid();
        // RC 锚点声明集合（geo/tex/mat，与组装器同口径）
        Set<String> declared = new HashSet<>();
        DeclarationTables.collectRefs(main).values()
                .forEach(list -> list.forEach(n -> declared.add(n.uid())));
        for (NodeInstance node : main.nodes()) {
            if (NodeTypes.RC_DECLARATION_PORTS.containsKey(node.type())) {
                if (rawIdentifier(node).isEmpty() || declared.contains(node.uid())) {
                    continue;
                }
                out.add(Diagnostic.warning(REF_NOT_CONNECTED,
                        node.type() + " 未接入 rc.root 的 geometry/textures/materials 端口"
                                + "（或 ref.rc 的声明端口），不会进入声明表",
                        node.uid()));
            } else if (NodeTypes.ENTITY_DECLARATION_PORTS.containsKey(node.type())) {
                String port = NodeTypes.ENTITY_DECLARATION_PORTS.get(node.type());
                if (rawIdentifier(node).isEmpty()) {
                    continue;
                }
                boolean wired = main.wires().stream().anyMatch(w ->
                        w.from().node().equals(node.uid()) && w.to().node().equals(rootUid)
                                && w.to().port().equals(port));
                if (wired) {
                    continue;
                }
                boolean animateOnly = main.wires().stream().anyMatch(w ->
                        w.from().node().equals(node.uid()) && w.to().port().equals("ref")
                                && main.findNode(w.to().node())
                                .map(n -> n.type().equals("animate.entry")).orElse(false));
                out.add(Diagnostic.warning(REF_NOT_CONNECTED, animateOnly
                        ? node.type() + " 仅连接 animate.entry，不会进入声明表；请同时连线 entity.root 的 "
                                + port + " 端口"
                        : node.type() + " 未连线 entity.root 的 " + port + " 声明端口，不会进入声明表",
                        node.uid()));
            } else if (node.type().equals("ref.rc") || node.type().equals("rc.root")) {
                if (node.type().equals("ref.rc") && rawIdentifier(node).isEmpty()) {
                    continue;
                }
                String outPort = node.type().equals("ref.rc") ? "ref" : "controller";
                boolean connected = main.wires().stream().anyMatch(w ->
                        w.from().node().equals(node.uid()) && w.from().port().equals(outPort)
                                && w.to().node().equals(rootUid) && w.to().port().equals("render_controllers"));
                if (!connected) {
                    out.add(Diagnostic.warning(REF_NOT_CONNECTED,
                            node.type() + " 的 " + outPort + " 未连接 entity.root 的 render_controllers 端口，"
                                    + "不会出现在 render_controllers", node.uid()));
                }
            }
        }
        // 子图中的声明类 ref（无法声明）→ 提示移至主图接线
        for (Map.Entry<String, GraphData> e : library.graphs().entrySet()) {
            if (e.getKey().equals(library.main())) {
                continue;
            }
            for (NodeInstance node : e.getValue().nodes()) {
                boolean isDeclRef = NodeTypes.RC_DECLARATION_PORTS.containsKey(node.type())
                        || NodeTypes.ENTITY_DECLARATION_PORTS.containsKey(node.type());
                if (!isDeclRef || rawIdentifier(node).isEmpty()) {
                    continue;
                }
                out.add(Diagnostic.warning(REF_NOT_CONNECTED,
                        node.type() + " 位于子图 '" + e.getKey() + "'，无法进入声明表；"
                                + "请移至主图并接入 RenderController 或 entity.root", node.uid()));
            }
        }
    }

    /** ref 节点的实例级标识符选项原文（无实例值 = 空，不落类型默认；占位 ref 判定用）。 */
    private static String rawIdentifier(NodeInstance node) {
        String optionId = node.type().equals("ref.rc") ? "identifier" : ShortNames.valueOptionOf(node.type());
        if (optionId == null) {
            return "";
        }
        return node.optionRaw(optionId).map(JsonElement::getAsString).orElse("");
    }

    // ====================================================================
    // 单图验证
    // ====================================================================

    /**
     * 验证单张图（库级检查除外）。{@code graphName} 仅用于诊断消息。
     */
    public static List<Diagnostic> validateGraph(GraphLibrary library, String graphName, GraphData graph) {
        List<Diagnostic> out = new ArrayList<>();
        NodeType.SubgraphResolver resolver = NodeType.SubgraphResolver.of(library, graph.graphInterface());

        // 检查 2：uid 重复；建立 uid → 节点索引（保留首次出现）
        Map<String, NodeInstance> byUid = new LinkedHashMap<>();
        for (NodeInstance node : graph.nodes()) {
            if (byUid.putIfAbsent(node.uid(), node) != null) {
                out.add(Diagnostic.error(DUPLICATE_UID, "节点 uid 重复: " + node.uid(), node.uid()));
            }
        }

        // 检查 1：未知节点类型；推导每实例端口集
        Map<String, NodeType> types = new HashMap<>();
        Map<String, List<PortDef>> inputs = new HashMap<>();
        Map<String, List<PortDef>> outputs = new HashMap<>();
        for (NodeInstance node : byUid.values()) {
            Optional<NodeType> type = NodeTypes.get(node.type());
            if (type.isEmpty()) {
                out.add(Diagnostic.error(UNKNOWN_NODE_TYPE, "未知节点类型: " + node.type(), node.uid()));
                continue;
            }
            types.put(node.uid(), type.get());
            inputs.put(node.uid(), type.get().inputsOf(node, resolver));
            outputs.put(node.uid(), type.get().outputsOf(node, resolver));
        }

        // wire 检查（3/4/5/6/10/11）+ 度数统计 + 有效边收集
        Map<PortRef, PortDef> inPorts = new HashMap<>();   // 合法 to 端点 → 端口定义
        Map<PortRef, PortDef> outPorts = new HashMap<>();  // 合法 from 端点 → 端口定义
        Map<PortRef, Integer> inDegree = new HashMap<>();
        Map<PortRef, Integer> outDegree = new HashMap<>();
        List<Wire> valueExecEdges = new ArrayList<>();     // 非 SLOT 边（环检测）
        List<Wire> execEdges = new ArrayList<>();          // EXEC 边（孤儿链）
        List<Wire> allEdges = new ArrayList<>();           // 全部合法边（可达性）

        for (Wire wire : graph.wires()) {
            Endpoint from = resolveEndpoint(wire.from(), byUid, types, inputs, outputs, out);
            Endpoint to = resolveEndpoint(wire.to(), byUid, types, inputs, outputs, out);
            if (from == null || to == null) {
                continue;
            }
            // 检查 4：方向
            if (!from.output() || to.output()) {
                out.add(Diagnostic.error(WIRE_DIRECTION,
                        "连线方向错误：" + wire.from() + "（应为 OUT）→ " + wire.to() + "（应为 IN）",
                        from.output() ? wire.to().node() : wire.from().node()));
                continue;
            }
            PortType fromType = from.port().type();
            PortType toType = to.port().type();
            // 检查 5：类型兼容（ANY 参与时 isAssignableTo 恒 true，自然豁免）
            if (!fromType.isAssignableTo(toType)) {
                out.add(Diagnostic.error(TYPE_MISMATCH,
                        "类型不兼容：" + wire.from() + "（" + fromType + "）→ " + wire.to() + "（" + toType + "）",
                        wire.to().node()));
            }
            // 检查 10：SLOT 装配白名单
            if (toType == PortType.SLOT) {
                String expected = SLOT_WHITELIST.get(to.node().type() + "." + to.port().id());
                if (expected == null || !expected.equals(from.node().type())) {
                    out.add(Diagnostic.error(SLOT_KIND,
                            "SLOT 连线种类非法：" + from.node().type() + " → " + to.node().type() + "." + to.port().id()
                                    + (expected != null ? "（应为 " + expected + "）" : "（该端口不接受 SLOT 连线）"),
                            to.node().uid()));
                }
            }
            // 检查 11：资源引用误用
            checkRefMisuse(wire, from, to, out);

            inPorts.put(wire.to(), to.port());
            outPorts.put(wire.from(), from.port());
            inDegree.merge(wire.to(), 1, Integer::sum);
            outDegree.merge(wire.from(), 1, Integer::sum);
            allEdges.add(wire);
            if (fromType != PortType.SLOT && toType != PortType.SLOT
                    // v9：variable.in 写入通道不成环（v.x = v.x + 1 是合法模式）
                    && !NodeTypes.isVariableWriteInput(to.node().type(), wire.to().port())) {
                valueExecEdges.add(wire);
            }
            if (fromType == PortType.EXEC) {
                execEdges.add(wire);
            }
        }

        // 检查 7：非 multi 输入被连多条
        for (Map.Entry<PortRef, Integer> e : inDegree.entrySet()) {
            PortDef port = inPorts.get(e.getKey());
            if (e.getValue() > 1 && port != null && !port.multi()) {
                out.add(Diagnostic.error(DUPLICATE_INPUT,
                        "非 multi 输入端口被连接 " + e.getValue() + " 条：" + e.getKey(), e.getKey().node()));
            }
        }
        // 检查 8：exec 输出 fan-out
        for (Map.Entry<PortRef, Integer> e : outDegree.entrySet()) {
            PortDef port = outPorts.get(e.getKey());
            if (e.getValue() > 1 && port != null && port.type() == PortType.EXEC) {
                out.add(Diagnostic.error(EXEC_FANOUT,
                        "exec 输出端口出度 " + e.getValue() + " > 1：" + e.getKey(), e.getKey().node()));
            }
        }

        // 检查 9：环检测（值边 + exec 边，SLOT 边不参与）
        detectValueCycle(byUid, valueExecEdges, out);

        // 可达性（根锚点出发，沿全部合法边反向遍历）
        Set<String> roots = new LinkedHashSet<>();
        for (NodeInstance node : byUid.values()) {
            if (ROOT_ANCHORS.contains(node.type())) {
                roots.add(node.uid());
            }
        }
        Set<String> reachable = reverseReachable(roots, allEdges);
        Set<String> execReachable = reverseReachable(roots, execEdges);

        // 检查 17：可达节点的未连接输入
        for (String uid : reachable) {
            NodeInstance node = byUid.get(uid);
            List<PortDef> ports = inputs.get(uid);
            if (node == null || ports == null) {
                continue;
            }
            // 根锚点节点的输入是「装配槽」：未连线 = 字段缺省（assembler 决定输出与否），不是错误。
            if (ASSEMBLY_ROOTS.contains(node.type())) {
                continue;
            }
            for (PortDef port : ports) {
                if (port.type() == PortType.EXEC || port.type() == PortType.SLOT) {
                    continue;
                }
                // multi 输入是声明通道（decl_*）、ref 的 read:/write: 是变量声明通道：
                // 未连线 = 空声明，不是错误
                if (port.multi() || NodeTypes.isVarRefPort(port.id())) {
                    continue;
                }
                if (inDegree.containsKey(new PortRef(uid, port.id()))) {
                    continue;
                }
                if (node.constants().containsKey(port.id()) || port.defaultValue().isPresent()) {
                    continue;
                }
                out.add(Diagnostic.error(UNCONNECTED_INPUT,
                        "输入端口未连接且无内联值/默认值：" + uid + "." + port.id(), uid));
            }
        }

        // 检查 18：孤儿 exec 链（存在根锚点才检查，避免无主图时报全图）
        if (!roots.isEmpty()) {
            for (Map.Entry<String, NodeInstance> e : byUid.entrySet()) {
                if (execReachable.contains(e.getKey())) {
                    continue;
                }
                boolean hasExec = inputs.getOrDefault(e.getKey(), List.of()).stream()
                        .anyMatch(p -> p.type() == PortType.EXEC)
                        || outputs.getOrDefault(e.getKey(), List.of()).stream()
                        .anyMatch(p -> p.type() == PortType.EXEC);
                if (hasExec) {
                    out.add(Diagnostic.warning(ORPHAN_CHAIN,
                            "exec 链节点未连入任何槽：" + e.getKey() + "（" + e.getValue().type() + "）", e.getKey()));
                }
            }
        }

        // 检查 19：未声明的黑板变量引用
        checkVariableRefs(graph, byUid, types, out);

        // 检查 20：exec.set_var target 必须是 variable 节点
        checkSetVarTargets(graph, byUid, types, out);

        return out;
    }

    /** wire 端点解析结果。 */
    private record Endpoint(NodeInstance node, PortDef port, boolean output) {
    }

    /**
     * 解析 wire 端点：节点必须存在、端口 id 必须在该实例端口集内（动态端口经 resolver 推导）。
     * 失败时产生 {@link #UNKNOWN_WIRE_ENDPOINT} 诊断并返回 null。
     */
    private static @Nullable Endpoint resolveEndpoint(PortRef ref, Map<String, NodeInstance> byUid,
                                            Map<String, NodeType> types,
                                            Map<String, List<PortDef>> inputs,
                                            Map<String, List<PortDef>> outputs,
                                            List<Diagnostic> out) {
        NodeInstance node = byUid.get(ref.node());
        if (node == null) {
            out.add(Diagnostic.error(UNKNOWN_WIRE_ENDPOINT, "连线端点节点不存在：" + ref.node(), ref.node()));
            return null;
        }
        if (!types.containsKey(ref.node())) {
            return null; // 未知类型已报 UNKNOWN_NODE_TYPE
        }
        Optional<PortDef> asOut = findPort(outputs.getOrDefault(ref.node(), List.of()), ref.port());
        if (asOut.isPresent()) {
            return new Endpoint(node, asOut.get(), true);
        }
        Optional<PortDef> asIn = findPort(inputs.getOrDefault(ref.node(), List.of()), ref.port());
        if (asIn.isPresent()) {
            return new Endpoint(node, asIn.get(), false);
        }
        out.add(Diagnostic.error(UNKNOWN_WIRE_ENDPOINT,
                "连线端点端口不存在：" + ref + "（节点类型 " + node.type() + "）", ref.node()));
        return null;
    }

    private static Optional<PortDef> findPort(List<PortDef> ports, String id) {
        return ports.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    /** 检查 11：资源引用节点的输出只能接规定目标。 */
    private static void checkRefMisuse(Wire wire, Endpoint from, Endpoint to, List<Diagnostic> out) {
        String fromType = from.node().type();
        if (ASSEMBLY_ONLY_REFS.contains(fromType)) {
            // ref.animation/ref.ac：animate.entry.ref 或 entity.root 动画声明端口（v4）
            String declarationPort = NodeTypes.ENTITY_DECLARATION_PORTS.get(fromType);
            boolean ok = (to.node().type().equals("animate.entry") && to.port().id().equals("ref"))
                    || (declarationPort != null && to.node().type().equals("entity.root")
                            && to.port().id().equals(declarationPort));
            if (!ok) {
                out.add(Diagnostic.error(REF_MISUSE,
                        fromType + " 的输出只能连接 animate.entry.ref / entity.root." + declarationPort
                                + "，实际连接 " + wire.to(), from.node().uid()));
            }
        } else if (fromType.equals("ref.particle")) {
            // ref.particle：particle.entry.ref 或 entity.root.particles（v10）
            boolean ok = (to.node().type().equals("particle.entry") && to.port().id().equals("ref"))
                    || (to.node().type().equals("entity.root") && to.port().id().equals("particles"));
            if (!ok) {
                out.add(Diagnostic.error(REF_MISUSE,
                        "ref.particle 的输出只能连接 particle.entry.ref / entity.root.particles，实际连接 "
                                + wire.to(), from.node().uid()));
            }
        } else if (fromType.equals("ref.sound")) {
            // ref.sound：entity.root.sounds 或 ac.state.sounds（v10）
            boolean ok = (to.node().type().equals("entity.root") && to.port().id().equals("sounds"))
                    || (to.node().type().equals("ac.state") && to.port().id().equals("sounds"));
            if (!ok) {
                out.add(Diagnostic.error(REF_MISUSE,
                        "ref.sound 的输出只能连接 entity.root.sounds / ac.state.sounds，实际连接 "
                                + wire.to(), from.node().uid()));
            }
        } else if (fromType.equals("ref.rc")) {
            // ref.rc：只能接 entity.root.render_controllers（v4）
            boolean ok = to.node().type().equals("entity.root")
                    && to.port().id().equals("render_controllers");
            if (!ok) {
                out.add(Diagnostic.error(REF_MISUSE,
                        "ref.rc 的输出只能连接 entity.root.render_controllers，实际连接 " + wire.to(),
                        from.node().uid()));
            }
        } else if (fromType.equals("rc.root") && from.port().id().equals("controller")) {
            // rc.root.controller：只能接 entity.root.render_controllers（v4）
            boolean ok = to.node().type().equals("entity.root")
                    && to.port().id().equals("render_controllers");
            if (!ok) {
                out.add(Diagnostic.error(REF_MISUSE,
                        "rc.root 的 controller 只能连接 entity.root.render_controllers，实际连接 " + wire.to(),
                        from.node().uid()));
            }
        } else if (VALUE_REFS.contains(fromType)) {
            // 声明端口类型（GEOMETRY_REF 等）本身是值类型，isValue 天然放行；接错类别由类型检查报
            if (!to.port().type().isValue()) {
                out.add(Diagnostic.error(REF_MISUSE,
                        fromType + " 的输出只能连接表达式槽、material.entry.value / list.entry.value 或 RC 锚点的 "
                                + NodeTypes.RC_DECLARATION_PORTS.get(fromType)
                                + " 声明端口，实际连接 " + wire.to(), from.node().uid()));
            }
        }
    }

    /** 检查 9：值边 + exec 边上的环（SLOT 边不参与）。 */
    private static void detectValueCycle(Map<String, NodeInstance> byUid, List<Wire> edges, List<Diagnostic> out) {
        Map<String, List<String>> adj = new HashMap<>();
        for (Wire wire : edges) {
            adj.computeIfAbsent(wire.from().node(), k -> new ArrayList<>()).add(wire.to().node());
        }
        Map<String, Integer> color = new HashMap<>();
        for (String uid : byUid.keySet()) {
            if (color.getOrDefault(uid, 0) == 0) {
                detectValueCycleDfs(uid, adj, color, out);
            }
        }
    }

    private static void detectValueCycleDfs(String start, Map<String, List<String>> adj,
                                            Map<String, Integer> color, List<Diagnostic> out) {
        record Frame(String uid, int next) {
        }
        Deque<Frame> frames = new ArrayDeque<>();
        color.put(start, 1);
        frames.push(new Frame(start, 0));
        while (!frames.isEmpty()) {
            Frame top = frames.peek();
            List<String> neighbors = adj.getOrDefault(top.uid(), List.of());
            if (top.next() < neighbors.size()) {
                frames.pop();
                frames.push(new Frame(top.uid(), top.next() + 1));
                String v = neighbors.get(top.next());
                int c = color.getOrDefault(v, 0);
                if (c == 1) {
                    out.add(Diagnostic.error(CYCLE, "数据流/执行流存在环，途经节点：" + v, v));
                } else if (c == 0) {
                    color.put(v, 1);
                    frames.push(new Frame(v, 0));
                }
            } else {
                frames.pop();
                color.put(top.uid(), 2);
            }
        }
    }

    /** 从根锚点沿边反向（to → from）可达的节点集合。 */
    private static Set<String> reverseReachable(Set<String> roots, List<Wire> edges) {
        Map<String, List<String>> reverse = new HashMap<>();
        for (Wire wire : edges) {
            reverse.computeIfAbsent(wire.to().node(), k -> new ArrayList<>()).add(wire.from().node());
        }
        Set<String> visited = new LinkedHashSet<>(roots);
        Deque<String> queue = new ArrayDeque<>(roots);
        while (!queue.isEmpty()) {
            String uid = queue.poll();
            for (String prev : reverse.getOrDefault(uid, List.of())) {
                if (visited.add(prev)) {
                    queue.add(prev);
                }
            }
        }
        return visited;
    }

    /** 检查 19：variable 节点引用未声明的黑板变量。 */
    private static void checkVariableRefs(GraphData graph, Map<String, NodeInstance> byUid,
                                          Map<String, NodeType> types, List<Diagnostic> out) {
        Set<String> declared = new HashSet<>();
        for (VariableDecl var : graph.variables()) {
            declared.add(var.name());
            // 检查 19c（规格 nodegraph-variable-table §2.6）：未选类型的声明（unknown 过渡态）
            if (var.type() == PortType.UNKNOWN) {
                out.add(Diagnostic.warning(VARIABLE_TYPE_UNKNOWN,
                        "变量未选择类型（unknown）：" + var.name()));
            }
        }
        for (NodeInstance node : byUid.values()) {
            NodeType type = types.get(node.uid());
            if (type == null || type.kind() != NodeType.Kind.VARIABLE) {
                continue;
            }
            String name = node.option("name", type).map(JsonElement::getAsString).orElse("");
            String stripped = name.startsWith("variable.") ? name.substring("variable.".length()) : name;
            if (!declared.contains(stripped)) {
                out.add(Diagnostic.warning(UNDECLARED_VARIABLE,
                        "引用了黑板未声明的变量：" + name, node.uid()));
            }
        }

        // 检查 19b（规格 nodegraph-variable-table §2.5）：TEMP 作用域变量同图有读无写——
        // temp 仅在当次求值内存活，跨求值读恒为 0。
        Set<String> tempDeclared = new HashSet<>();
        for (VariableDecl var : graph.variables()) {
            if (var.scope() == VariableDecl.Scope.TEMP) {
                tempDeclared.add(var.name());
            }
        }
        if (!tempDeclared.isEmpty()) {
            Set<String> written = new HashSet<>();
            Set<String> read = new HashSet<>();
            Map<String, String> varNodeName = new HashMap<>();
            for (NodeInstance node : byUid.values()) {
                NodeType type = types.get(node.uid());
                if (type != null && type.kind() == NodeType.Kind.VARIABLE) {
                    String name = node.option("name", type).map(JsonElement::getAsString).orElse("");
                    varNodeName.put(node.uid(), name.startsWith("variable.")
                            ? name.substring("variable.".length()) : name);
                }
            }
            for (Wire w : graph.wires()) {
                // v9 左读右写：写 = exec.set_var.target 输出 → variable.in；读 = variable.out 的全部出线
                String sinkName = "in".equals(w.to().port()) ? varNodeName.get(w.to().node()) : null;
                if (sinkName != null && tempDeclared.contains(sinkName)) {
                    NodeType producerType = types.get(w.from().node());
                    if (producerType != null && producerType.kind() == NodeType.Kind.EXEC_SET_VAR
                            && w.from().port().equals("target")) {
                        written.add(sinkName);
                    }
                    continue;
                }
                String sourceName = "out".equals(w.from().port()) ? varNodeName.get(w.from().node()) : null;
                if (sourceName == null || !tempDeclared.contains(sourceName)) {
                    continue;
                }
                read.add(sourceName);
            }
            for (String name : read) {
                if (!written.contains(name)) {
                    out.add(Diagnostic.warning(TEMP_NEVER_WRITTEN,
                            "temp 作用域变量被读但同图无写入（跨求值读恒为 0）：" + name));
                }
            }
        }
    }

    /** 检查 20：exec.set_var 的 target 引脚必须连线到 variable 节点（写身份）。 */
    private static void checkSetVarTargets(GraphData graph, Map<String, NodeInstance> byUid,
                                           Map<String, NodeType> types, List<Diagnostic> out) {
        for (NodeInstance node : byUid.values()) {
            NodeType type = types.get(node.uid());
            if (type == null || type.kind() != NodeType.Kind.EXEC_SET_VAR) {
                continue;
            }
            boolean ok = false;
            for (Wire wire : graph.wires()) {
                if (wire.from().node().equals(node.uid()) && wire.from().port().equals("target")) {
                    NodeInstance consumer = byUid.get(wire.to().node());
                    NodeType consumerType = consumer == null ? null : types.get(consumer.uid());
                    // v9：target（右侧输出）必须接到 variable 节点的 in（写入通道）
                    ok = consumerType != null && consumerType.kind() == NodeType.Kind.VARIABLE
                            && wire.to().port().equals("in");
                    break;
                }
            }
            if (!ok) {
                out.add(Diagnostic.error(SET_TARGET_NOT_VARIABLE,
                        "exec.set_var 的 target 必须连线到 variable 节点", node.uid()));
            }
        }
    }
}
