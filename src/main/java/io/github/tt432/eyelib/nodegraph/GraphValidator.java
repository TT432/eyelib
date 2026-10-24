package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;

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
    /** event.* 节点放在非 client_entity 主图（v13，规格 nodegraph-event-nodes）。 */
    public static final String EVENT_GRAPH_KIND = "EVENT_GRAPH_KIND";
    /** 同类 event 节点在主图出现超过 1 个（v13，规格 nodegraph-event-nodes）。 */
    public static final String EVENT_DUPLICATE = "EVENT_DUPLICATE";

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

    /** 根锚点节点类型 id（可达性分析起点，沿边反向：谁喂给根）。 */
    private static final Set<String> ROOT_ANCHORS = Set.of("entity.root", "rc.root", "ac.root", "subgraph.output");

    /** 时机源锚点（v13，规格 nodegraph-event-nodes）：exec 链从此出发，
     *  可达性分析沿边正向；ac.state 的 on_entry/on_exit 是 OUT 源端口。 */
    private static final Set<String> SOURCE_ANCHORS = Set.of(
            "event.initialize", "event.pre_animation", "event.parent_setup", "ac.state");

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
        return validate(library, RefExistenceChecker.alwaysExists());
    }

    /**
     * 验证整个库（带资产存在性）：ref 节点引用的资产不存在时报 MISSING_REFERENCE（ERROR）。
     * 存在性由调用方注入（域不依赖客户端注册表）。
     */
    public static List<Diagnostic> validate(GraphLibrary library, RefExistenceChecker refExistence) {
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
        SubgraphValidator.checkSubgraphAnchors(library, out);
        Map<String, List<SubgraphValidator.CallEdge>> callGraph = SubgraphValidator.checkSubgraphTargets(library, out);
        SubgraphValidator.checkSubgraphRecursion(library, callGraph, out);
        if (main != null) {
            checkRefConflicts(library, callGraph, out);
            checkRefNotConnected(library, out);
            checkDuplicateRcIds(library, out);
            EntryChainValidator.checkEntryChains(library, out);
        }
        checkMissingReferences(library, refExistence, out);
        return out;
    }

    /** 检查 24（ERROR）：ref 节点引用的资产不存在（规格 nodegraph-missing-refs）。 */
    public static final String MISSING_REFERENCE = "MISSING_REFERENCE";

    /** ref 节点类型 id → 引用值选项名。 */
    private static final Map<String, String> REF_VALUE_OPTIONS = Map.of(
            "ref.geometry", "identifier",
            "ref.texture", "path",
            "ref.material", "material",
            "ref.animation", "identifier",
            "ref.ac", "identifier",
            "ref.particle", "identifier",
            "ref.sound", "identifier",
            "ref.rc", "identifier");

    private static void checkMissingReferences(GraphLibrary library, RefExistenceChecker checker,
                                               List<Diagnostic> out) {
        for (GraphData graph : library.graphs().values()) {
            for (NodeInstance node : graph.nodes()) {
                String optionName = REF_VALUE_OPTIONS.get(node.type());
                if (optionName == null) {
                    continue;
                }
                // 选项无实例值 = 占位 ref（example 默认值），跳过（与 UNKNOWN_REFERENCE 同口径）
                String value = node.options().containsKey(optionName)
                        ? node.options().get(optionName).getAsString() : "";
                if (value.isBlank()) {
                    continue;
                }
                if (!checker.exists(node.type(), value)) {
                    out.add(Diagnostic.error(MISSING_REFERENCE,
                            node.type() + " 引用的资产不存在: " + value, node.uid()));
                }
            }
        }
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

    /**
     * 检查 16：主图可达全部图内 ref 的有效短名合法性（INVALID_SHORT_NAME）；
     * REF_CONFLICT 范围 = 主图连到 entity.root 声明端口的 ref（规格 D1：声明 = 连线），
     * 同有效短名不同标识 = 冲突。
     */
    private static void checkRefConflicts(GraphLibrary library, Map<String, List<SubgraphValidator.CallEdge>> callGraph,
                                          List<Diagnostic> out) {
        // BFS 收集可达图
        Set<String> reachable = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        reachable.add(library.main());
        queue.add(library.main());
        while (!queue.isEmpty()) {
            String name = queue.poll();
            for (SubgraphValidator.CallEdge edge : callGraph.getOrDefault(name, List.of())) {
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
            GraphEndpoints.Endpoint from = GraphEndpoints.resolveEndpoint(
                    wire.from(), byUid, types, inputs, outputs, out);
            GraphEndpoints.Endpoint to = GraphEndpoints.resolveEndpoint(
                    wire.to(), byUid, types, inputs, outputs, out);
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
            GraphEndpoints.checkRefMisuse(wire, from, to, out);

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
        GraphReachability.detectValueCycle(byUid, valueExecEdges, out);

        // 可达性（根锚点反向 + 时机源锚点正向，v13）
        Set<String> roots = new LinkedHashSet<>();
        Set<String> sources = new LinkedHashSet<>();
        for (NodeInstance node : byUid.values()) {
            if (ROOT_ANCHORS.contains(node.type())) {
                roots.add(node.uid());
            }
            if (SOURCE_ANCHORS.contains(node.type())) {
                sources.add(node.uid());
            }
        }
        Set<String> reachable = GraphReachability.reverseReachable(roots, allEdges);
        reachable.addAll(GraphReachability.forwardReachable(sources, allEdges));
        Set<String> execReachable = GraphReachability.reverseReachable(roots, execEdges);
        execReachable.addAll(GraphReachability.forwardReachable(sources, execEdges));

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

        // 检查 18：孤儿 exec 链（存在锚点才检查，避免无主图时报全图）
        if (!roots.isEmpty() || !sources.isEmpty()) {
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
                            "exec 链节点未接入任何执行时机锚点或槽：" + e.getKey() + "（" + e.getValue().type() + "）", e.getKey()));
                }
            }
        }

        // 检查 19：未声明的黑板变量引用
        VariableRefValidator.checkVariableRefs(graph, byUid, types, out);

        // 检查 20：exec.set_var target 必须是 variable 节点
        VariableRefValidator.checkSetVarTargets(graph, byUid, types, out);

        // 检查 21：event.* 时机节点图种与数量约束（v13，规格 nodegraph-event-nodes）
        checkEventNodes(library, graphName, byUid, out);

        return out;
    }

    /**
     * 检查 21：event.* 节点只能出现在 CLIENT_ENTITY 库的主图，且同类至多 1 个
     * （用户决策 2026-08-15：同类事件唯一，语义最清晰）。
     */
    private static void checkEventNodes(GraphLibrary library, String graphName,
                                        Map<String, NodeInstance> byUid, List<Diagnostic> out) {
        Map<String, Integer> eventCounts = new LinkedHashMap<>();
        for (NodeInstance node : byUid.values()) {
            if (NodeTypes.EVENT_SCRIPT_SLOTS.containsKey(node.type())) {
                eventCounts.merge(node.type(), 1, Integer::sum);
            }
        }
        if (eventCounts.isEmpty()) {
            return;
        }
        boolean entityMain = library.kind() == GraphKind.CLIENT_ENTITY && graphName.equals(library.main());
        if (!entityMain) {
            for (NodeInstance node : byUid.values()) {
                if (NodeTypes.EVENT_SCRIPT_SLOTS.containsKey(node.type())) {
                    out.add(Diagnostic.error(EVENT_GRAPH_KIND,
                            "event 节点 " + node.type() + " 只能放在 client_entity 库的主图（当前库种类 "
                                    + library.kind().getSerializedName() + "，图 '" + graphName + "'）", node.uid()));
                }
            }
            return;
        }
        for (Map.Entry<String, Integer> e : eventCounts.entrySet()) {
            if (e.getValue() > 1) {
                out.add(Diagnostic.error(EVENT_DUPLICATE,
                        "同类 event 节点每张主图至多 1 个：" + e.getKey() + " 实际 " + e.getValue()));
            }
        }
    }

}
