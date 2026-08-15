package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/**
 * 图文档迁移：按 format_version 链式执行（规格 nodegraph-eproject-variables §3.4、
 * nodegraph-declaration-wiring §2.4）。
 *
 * <p>v1 → v2 变量节点化（无兼容双轨）：
 * <ul>
 *   <li>{@code var.get}（name=variable.foo）→ {@code variable}（name=foo，不带根）；</li>
 *   <li>{@code exec.set_var} root=variable → 新 {@code exec.set_var}（target 引脚）
 *       + 自动生成 variable 节点（置于原节点左上）连线 target；</li>
 *   <li>{@code exec.set_var} root=temp → {@code exec.set_temp}（name 保留）。</li>
 * </ul>
 *
 * <p>v2 → v3 声明连线化（CLIENT_ENTITY 库；声明 = 连线，扫描语义废止）：
 * <ul>
 *   <li>主图无声明连线的 ref.{geometry,texture,material,animation,ac} → 补线到 entity.root
 *       对应声明端口；</li>
 *   <li>主图未连任何 rc.condition_entry 的 ref.rc → 新建 rc.condition_entry（置于 ref 右侧，
 *       condition 用端口默认 1），ref→entry.rc、entry.entry→root.render_controllers；</li>
 *   <li>子图中完全无连线的声明类 ref → 移到主图（uid/选项保留，置于主图空闲区）并补线；
 *       有连线的不动（验证器 REF_NOT_CONNECTED 提示）；</li>
 *   <li>RENDER_CONTROLLER / ANIMATION_CONTROLLER 库不变。</li>
 * </ul>
 *
 * <p>v3 → v4 RC 内联（CLIENT_ENTITY 库）：rc.condition_entry 拆解、声明线重定向 ref.rc。
 *
 * <p>v4 → v5 颜色端口化（所有库）：rc.root 16 个 float 通道端口 → 4 个 COLOR 端口
 * （常量 → const.color，含表达式 → color.compose）。
 *
 * <p>v5 → v6 有序条目链 + decl_* 移除（规格 nodegraph-ordered-entries-and-rc-reference-set）：
 * <ul>
 *   <li>rc.root 的 textures/materials/part_visibility 多条目线 → 按 uid 序（= v5 发射序）
 *       排成链：首条目线保留，其余重定向为 e_i.entry → e_{i-1}.next；</li>
 *   <li>rc.root 的 decl_* 连线删除（ref 节点保留；协议短名由 DeclarationTables carve-out
 *       接管，非协议行自此不进表）；</li>
 *   <li>ref.rc 的 decl_* 不变。</li>
 * </ul>
 *
 * <p>v6 → v7 命名变量端口：剥除 decl_variables 桶接线与 declvar- 节点
 * （规格 nodegraph-animation-variable-refs §2.1；新接线需被引内容重建，重新导入自动恢复）。
 *
 * <p>v7 → v8 ref.ac 撤除命名变量端口（AC 图化）：剥除 ref.ac 的 var_refs 快照、
 * 指向其 read:/write: 端口的连线与失连 declvar- 节点；ref.animation 保留。
 *
 * <p>v8 → v9 左读右写：exec.set_var.target 与 ref.animation write:<名> 翻转为
 * 右侧输出，写入边接 variable 节点新增的 in 端口；写入边不参与环检测与布局分层。
 *
 * <p>v9 → v10 AC 图边化（规格 nodegraph-ac-graph-and-effects）：ac.root.initial_state
 * 与 ac.transition.target 字符串选项 → 按名解析的图边；新增 ref.particle/ref.sound
 * 与实体粒子/音效声明表（旧图无此数据，无迁移动作）。
 *
 * <p>v11 → v12 对象类型落地：变量声明与子图接口参数的 any（旧「object」选项的实际
 * 存储值）迁移为 object（struct）；any 自此回归纯通配语义。
 *
 * <p>v12 → v13 执行时机事件化（规格 nodegraph-event-nodes）：entity.root 的
 * initialize/pre_animation/parent_setup EXEC 槽 → event.* 源节点（exec_out 引链）；
 * ac.state 的 on_entry/on_exit 翻转为 EXEC OUT 源端口。链拓扑不变，导出产物等价。
 *
 * <p>v13 → v14 操作符节点拆分：op.binary / op.unary 的 op 选项固化为节点类型身份
 * （op.add … op.or / op.negate / op.not，一符一类型），op 选项移除；端口与连线不变。
 *
 * <p>纯函数：输入输出均为不可变文档；加载路径（资源包 loader / EprojectIo）统一调用。
 * 已是新格式的文档原样返回。
 */
public final class GraphMigrations {
    private GraphMigrations() {
    }

    /** 迁移整个图库；format_version 升到 {@link GraphLibrary#CURRENT_FORMAT_VERSION}。 */
    public static GraphLibrary migrate(GraphLibrary library) {
        GraphLibrary result = library;
        if (result.formatVersion() < 2) {
            result = migrateV1ToV2(result);
        }
        if (result.formatVersion() < 3) {
            result = migrateV2ToV3(result);
        }
        if (result.formatVersion() < 4) {
            result = migrateV3ToV4(result);
        }
        if (result.formatVersion() < 5) {
            result = migrateV4ToV5(result);
        }
        if (result.formatVersion() < 6) {
            result = migrateV5ToV6(result);
        }
        if (result.formatVersion() < 7) {
            result = migrateV6ToV7(result);
        }
        if (result.formatVersion() < 8) {
            result = migrateV7ToV8(result);
        }
        if (result.formatVersion() < 9) {
            result = migrateV8ToV9(result);
        }
        if (result.formatVersion() < 10) {
            result = migrateV9ToV10(result);
        }
        if (result.formatVersion() < 11) {
            result = migrateV10ToV11(result);
        }
        if (result.formatVersion() < 12) {
            result = migrateV11ToV12(result);
        }
        if (result.formatVersion() < 13) {
            result = migrateV12ToV13(result);
        }
        if (result.formatVersion() < 14) {
            result = migrateV13ToV14(result);
        }
        return new GraphLibrary(GraphLibrary.CURRENT_FORMAT_VERSION, result.kind(), result.main(),
                result.graphs());
    }

    // ---------- v13 → v14：操作符节点拆分 ----------

    /**
     * v14 单操作符节点（用户决策 2026-08-16）：op.binary / op.unary 的 op 选项
     * 固化为节点类型身份（op.add / op.subtract / … / op.negate / op.not），选项移除；
     * 端口 id（a/b/out）与连线不变。未知或缺失 op 按旧默认回落（binary → "+"，unary → "-"）。
     */
    private static GraphLibrary migrateV13ToV14(GraphLibrary library) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            GraphData g = entry.getValue();
            List<NodeInstance> nodes = new ArrayList<>(g.nodes().size());
            for (NodeInstance node : g.nodes()) {
                nodes.add(migrateOpNode(node));
            }
            graphs.put(entry.getKey(), new GraphData(nodes, g.wires(), g.variables(),
                    g.placemats(), g.stickyNotes(), g.graphInterface()));
        }
        return new GraphLibrary(14, library.kind(), library.main(), graphs);
    }

    private static NodeInstance migrateOpNode(NodeInstance node) {
        boolean binary = node.type().equals("op.binary");
        boolean unary = node.type().equals("op.unary");
        if (!binary && !unary) {
            return node;
        }
        NodeType opType = binary
                ? NodeTypes.binaryOp(optionString(node, "op", "+"))
                : NodeTypes.unaryOp(optionString(node, "op", "-"));
        if (opType == null) {
            // 未知符号：回落旧默认（binary → op.add，unary → op.negate）
            opType = binary ? NodeTypes.binaryOp("+") : NodeTypes.unaryOp("-");
        }
        Map<String, JsonElement> options = new LinkedHashMap<>(node.options());
        options.remove("op");
        return new NodeInstance(node.uid(), opType.id(), node.x(), node.y(), options, node.constants());
    }

    // ---------- v12 → v13：执行时机事件化 ----------

    /**
     * v13 事件模型（规格 nodegraph-event-nodes，用户决策 2026-08-15）：
     * <ul>
     *   <li>entity.root 的 initialize/pre_animation/parent_setup EXEC IN 槽撤除，
     *       改为 event.* 源节点（仅 exec_out）：槽口连线（链尾 → 槽）改写为
     *       event 节点 → 链首 exec_in；无连线的槽不产节点；</li>
     *   <li>ac.state 的 on_entry/on_exit 从 IN 翻转为 OUT（端口 id 不变）：
     *       槽口连线（链尾 → state）改写为 state → 链首 exec_in。</li>
     * </ul>
     * 链内部拓扑（exec_out → exec_in 依序相连）不变，导出产物逐字节等价。
     */
    private static GraphLibrary migrateV12ToV13(GraphLibrary library) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            graphs.put(entry.getKey(), migrateEventsV13(entry.getValue()));
        }
        return new GraphLibrary(library.formatVersion(), library.kind(), library.main(), graphs);
    }

    private static GraphData migrateEventsV13(GraphData g) {
        Set<String> entityRootUids = new HashSet<>();
        Set<String> stateUids = new HashSet<>();
        for (NodeInstance n : g.nodes()) {
            if (NodeTypes.ENTITY_ROOT.id().equals(n.type())) {
                entityRootUids.add(n.uid());
            } else if (NodeTypes.AC_STATE.id().equals(n.type())) {
                stateUids.add(n.uid());
            }
        }
        // 旧槽口连线：(槽节点 uid, 槽端口) → 线；实体槽移植到 event 节点，state 槽翻转方向
        record OldSlotWire(Wire wire, boolean toEventNode) {
        }
        Map<String, OldSlotWire> slotWires = new LinkedHashMap<>();
        for (Wire w : g.wires()) {
            if (entityRootUids.contains(w.to().node())
                    && NodeTypes.EVENT_SCRIPT_SLOTS.containsValue(w.to().port())) {
                slotWires.put(w.to().node() + "#" + w.to().port(), new OldSlotWire(w, true));
            } else if (stateUids.contains(w.to().node())
                    && ("on_entry".equals(w.to().port()) || "on_exit".equals(w.to().port()))) {
                slotWires.put(w.to().node() + "#" + w.to().port(), new OldSlotWire(w, false));
            }
        }
        if (slotWires.isEmpty()) {
            return g;
        }
        Set<Wire> removed = new HashSet<>();
        List<Wire> added = new ArrayList<>();
        List<NodeInstance> newNodes = new ArrayList<>();
        Set<String> usedUids = new HashSet<>();
        for (NodeInstance n : g.nodes()) {
            usedUids.add(n.uid());
        }
        for (OldSlotWire slotWire : slotWires.values()) {
            Wire w = slotWire.wire();
            removed.add(w);
            // 沿 exec_in 反走找链首
            String head = w.from().node();
            while (true) {
                String prev = null;
                for (Wire back : g.wires()) {
                    if (back.to().node().equals(head) && "exec_in".equals(back.to().port())) {
                        prev = back.from().node();
                        break;
                    }
                }
                if (prev == null) {
                    break;
                }
                head = prev;
            }
            NodeInstance headNode = null;
            for (NodeInstance n : g.nodes()) {
                if (n.uid().equals(head)) {
                    headNode = n;
                    break;
                }
            }
            float x = headNode != null ? headNode.x() - 280 : 0;
            float y = headNode != null ? headNode.y() : 0;
            String sourceUid;
            String sourcePort;
            if (slotWire.toEventNode()) {
                sourceUid = uniqueUid("event_" + w.to().port(), usedUids);
                sourcePort = "exec_out";
                newNodes.add(new NodeInstance(sourceUid, "event." + w.to().port(), x, y,
                        Map.of(), Map.of()));
            } else {
                sourceUid = w.to().node();
                sourcePort = w.to().port();
            }
            added.add(new Wire(new PortRef(sourceUid, sourcePort), new PortRef(head, "exec_in")));
        }
        List<Wire> wires = new ArrayList<>();
        for (Wire w : g.wires()) {
            if (!removed.contains(w)) {
                wires.add(w);
            }
        }
        wires.addAll(added);
        List<NodeInstance> nodes = new ArrayList<>(g.nodes());
        nodes.addAll(newNodes);
        return new GraphData(nodes, wires, g.variables(), g.placemats(), g.stickyNotes(),
                g.graphInterface());
    }

    private static String uniqueUid(String base, Set<String> used) {
        String uid = base;
        int seq = 2;
        while (used.contains(uid)) {
            uid = base + "_" + (seq++);
        }
        used.add(uid);
        return uid;
    }

    // ---------- v11 → v12：变量声明 any → object ----------

    /**
     * v12 对象类型落地（规格 nodegraph-object-variables，用户决策 2026-08-14）：
     * v11 及以前变量声明/子图接口参数可选类型里的「object」实际存为 {@code any}
     * （ANY↔OBJECT 共用 LDLib2 OBJECT handle）；v12 起 object 是独立的 struct 类型
     * （{@link PortType#OBJECT}），any 回归纯通配语义。旧文件里声明为 any 的
     * 变量/接口参数全部视为 object 迁移——any 声明在旧版没有第三种语义来源
     * （新建占位是 unknown，导入产物的 any 同样是「对象/未细化」的展示语义）。
     */
    private static GraphLibrary migrateV11ToV12(GraphLibrary library) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            graphs.put(entry.getKey(), migrateAnyDeclsV12(entry.getValue()));
        }
        return new GraphLibrary(library.formatVersion(), library.kind(), library.main(), graphs);
    }

    private static GraphData migrateAnyDeclsV12(GraphData g) {
        List<VariableDecl> variables = new ArrayList<>(g.variables().size());
        boolean changed = false;
        for (VariableDecl v : g.variables()) {
            if (v.type() == PortType.ANY) {
                variables.add(new VariableDecl(v.name(), PortType.OBJECT, v.group(),
                        v.defaultValue(), v.scope()));
                changed = true;
            } else {
                variables.add(v);
            }
        }
        Optional<GraphInterface> iface = g.graphInterface();
        if (iface.isPresent()) {
            GraphInterface old = iface.get();
            List<GraphInterface.Param> inputs = new ArrayList<>(old.inputs().size());
            boolean ifaceChanged = false;
            for (GraphInterface.Param p : old.inputs()) {
                if (p.type() == PortType.ANY) {
                    inputs.add(new GraphInterface.Param(p.name(), PortType.OBJECT, p.defaultValue()));
                    ifaceChanged = true;
                } else {
                    inputs.add(p);
                }
            }
            GraphInterface.Param out = old.output();
            if (out.type() == PortType.ANY) {
                out = new GraphInterface.Param(out.name(), PortType.OBJECT, out.defaultValue());
                ifaceChanged = true;
            }
            if (ifaceChanged) {
                iface = Optional.of(new GraphInterface(List.copyOf(inputs), out));
                changed = true;
            }
        }
        if (!changed) {
            return g;
        }
        return new GraphData(g.nodes(), g.wires(), List.copyOf(variables), g.placemats(),
                g.stickyNotes(), iface);
    }

    // ---------- v10 → v11：变长 call 参数列表化 ----------

    /**
     * v11 变长参数列表化（规格 nodegraph-variadic-call-list，用户决策 2026-08-10）：
     * <ul>
     *   <li>arg_count 选项删除（定长由签名决定、变长由列表长度决定）；</li>
     *   <li>变长/未知函数的变长尾参 argN（序号 > 固定前缀长）的行内常量/const 连线
     *       收进 args 列表选项；连线源为非常量表达式的无法列表化，丢弃（实机普查零出现）；</li>
     *   <li>仅喂被收端口的孤儿 const 节点一并删除。</li>
     * </ul>
     */
    private static GraphLibrary migrateV10ToV11(GraphLibrary library) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            graphs.put(entry.getKey(), migrateCallArgsV11(entry.getValue()));
        }
        return new GraphLibrary(library.formatVersion(), library.kind(), library.main(), graphs);
    }

    private static GraphData migrateCallArgsV11(GraphData g) {
        record HarvestedPort(String uid, String port) {
        }
        List<NodeInstance> nodes = new ArrayList<>();
        Set<HarvestedPort> harvested = new HashSet<>();
        boolean any = false;
        for (NodeInstance n : g.nodes()) {
            if (!isCallNode(n.type())) {
                nodes.add(n);
                continue;
            }
            any = true;
            String function = n.optionString("function", "");
            var sig = io.github.tt432.eyelib.nodegraph.MolangFunctionSignatures.find(function);
            int fixed = sig != null ? sig.fixed().size() : 0;
            boolean variadic = sig == null || sig.varArg() != null;
            Map<String, JsonElement> options = new LinkedHashMap<>(n.options());
            options.remove("arg_count");
            Map<String, JsonElement> constants = new LinkedHashMap<>(n.constants());
            if (variadic) {
                // 按端口序号收集变长尾参值：先常量，后连线 const（非常量源丢弃）
                Map<Integer, JsonElement> byIndex = new TreeMap<>();
                for (var ce : constants.entrySet()) {
                    Integer idx = argIndex(ce.getKey());
                    if (idx != null && idx > fixed) {
                        byIndex.put(idx, ce.getValue());
                    }
                }
                for (Wire w : g.wires()) {
                    if (!w.to().node().equals(n.uid())) {
                        continue;
                    }
                    Integer idx = argIndex(w.to().port());
                    if (idx == null || idx <= fixed || byIndex.containsKey(idx)) {
                        continue;
                    }
                    NodeInstance src = g.findNode(w.from().node()).orElse(null);
                    if (src != null && src.type().startsWith("const.")
                            && src.options().containsKey("value")) {
                        byIndex.put(idx, src.options().get("value"));
                    }
                }
                for (int idx : byIndex.keySet()) {
                    constants.remove("arg" + idx);
                    harvested.add(new HarvestedPort(n.uid(), "arg" + idx));
                }
                if (!byIndex.isEmpty()) {
                    com.google.gson.JsonArray args = new com.google.gson.JsonArray();
                    byIndex.values().forEach(args::add);
                    options.put("args", args);
                }
            }
            nodes.add(new NodeInstance(n.uid(), n.type(), n.x(), n.y(), options, constants));
        }
        if (!any) {
            return g;
        }
        // 线清理：删掉落入被收端口的线；孤儿 const（只喂被收端口）一并删除
        List<Wire> wires = new ArrayList<>();
        Set<String> constCandidates = new HashSet<>();
        for (Wire w : g.wires()) {
            if (harvested.contains(new HarvestedPort(w.to().node(), w.to().port()))) {
                constCandidates.add(w.from().node());
                continue;
            }
            wires.add(w);
        }
        if (!constCandidates.isEmpty()) {
            Set<String> stillUsed = new HashSet<>();
            for (Wire w : wires) {
                stillUsed.add(w.from().node());
            }
            constCandidates.removeAll(stillUsed);
            if (!constCandidates.isEmpty()) {
                nodes.removeIf(n -> constCandidates.contains(n.uid()) && n.type().startsWith("const."));
            }
        }
        return new GraphData(nodes, wires, g.variables(), g.placemats(), g.stickyNotes(),
                g.graphInterface());
    }

    private static boolean isCallNode(String type) {
        return NodeTypes.QUERY_CALL.id().equals(type) || NodeTypes.MATH_CALL.id().equals(type)
                || NodeTypes.EXEC_CALL.id().equals(type);
    }

    /** "argN" → N；非 argN 形态 → null。 */
    private static @Nullable Integer argIndex(String port) {
        if (!port.startsWith("arg") || port.length() <= 3) {
            return null;
        }
        try {
            return Integer.parseInt(port.substring(3));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---------- v9 → v10：AC 图边化 ----------

    /**
     * v10 AC 图化（规格 nodegraph-ac-graph-and-effects，用户决策 2026-08-09）：
     * <ul>
     *   <li>ac.root.initial_state 字符串选项 → 按名解析 ac.state 建
     *       {@code state.state → ac.root.initial} 图边（解析失败丢选项）；</li>
     *   <li>ac.transition.target 字符串选项 → 按名建
     *       {@code transition.target → ac.state.incoming} 图边（解析失败丢选项）；</li>
     *   <li>实体/AC 库的粒子/音效表是 v10 新增支持，旧图无此数据，无迁移动作。</li>
     * </ul>
     */
    private static GraphLibrary migrateV9ToV10(GraphLibrary library) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            GraphData g = entry.getValue();
            Map<String, String> stateUids = new LinkedHashMap<>();
            for (NodeInstance n : g.nodes()) {
                if (NodeTypes.AC_STATE.id().equals(n.type())) {
                    stateUids.putIfAbsent(n.optionString("name", ""), n.uid());
                }
            }
            List<Wire> wires = new ArrayList<>(g.wires());
            List<NodeInstance> nodes = new ArrayList<>();
            for (NodeInstance n : g.nodes()) {
                String optionKey = NodeTypes.AC_ROOT.id().equals(n.type()) ? "initial_state"
                        : NodeTypes.AC_TRANSITION.id().equals(n.type()) ? "target" : null;
                if (optionKey != null && n.options().containsKey(optionKey)) {
                    String targetName = n.options().get(optionKey).getAsString();
                    String targetUid = stateUids.get(targetName);
                    if (targetUid != null) {
                        wires.add(NodeTypes.AC_ROOT.id().equals(n.type())
                                ? new Wire(new PortRef(targetUid, "state"), new PortRef(n.uid(), "initial"))
                                : new Wire(new PortRef(n.uid(), "target"), new PortRef(targetUid, "incoming")));
                    }
                    Map<String, JsonElement> options = new LinkedHashMap<>(n.options());
                    options.remove(optionKey);
                    nodes.add(new NodeInstance(n.uid(), n.type(), n.x(), n.y(), options, n.constants()));
                    continue;
                }
                nodes.add(n);
            }
            graphs.put(entry.getKey(), new GraphData(nodes, wires, g.variables(),
                    g.placemats(), g.stickyNotes(), g.graphInterface()));
        }
        return new GraphLibrary(library.formatVersion(), library.kind(), library.main(), graphs);
    }

    // ---------- v8 → v9：左读右写（写入通道翻转） ----------

    /**
     * v9 变量流向重排（用户决策 2026-08-09：左读右写，位置即语义）：
     * <ul>
     *   <li>exec.set_var.target 从左侧输入改为右侧输出：v8 连线
     *       {@code variable.out → set_var.target} 翻转为 {@code set_var.target → variable.in}；</li>
     *   <li>ref.animation 的 write:<名> 端口从输入改为输出：v8 连线
     *       {@code declvar.out → ref.write:<名>} 翻转为 {@code ref.write:<名> → declvar.in}；</li>
     *   <li>variable 节点新增 multi 输入 in（写入通道）。
     *       read: 端口方向不变。写入边自此不参与环检测与布局分层。</li>
     * </ul>
     */
    private static GraphLibrary migrateV8ToV9(GraphLibrary library) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            GraphData g = entry.getValue();
            Set<String> setVarUids = new HashSet<>();
            Set<String> animRefUids = new HashSet<>();
            for (NodeInstance n : g.nodes()) {
                if (NodeTypes.EXEC_SET_VAR.id().equals(n.type())) {
                    setVarUids.add(n.uid());
                } else if (NodeTypes.REF_ANIMATION.id().equals(n.type())) {
                    animRefUids.add(n.uid());
                }
            }
            List<Wire> wires = new ArrayList<>();
            for (Wire w : g.wires()) {
                if (setVarUids.contains(w.to().node()) && "target".equals(w.to().port())) {
                    wires.add(new Wire(new PortRef(w.to().node(), "target"),
                            new PortRef(w.from().node(), "in")));
                } else if (animRefUids.contains(w.to().node())
                        && w.to().port().startsWith(NodeTypes.VAR_WRITE_PREFIX)) {
                    wires.add(new Wire(new PortRef(w.to().node(), w.to().port()),
                            new PortRef(w.from().node(), "in")));
                } else {
                    wires.add(w);
                }
            }
            graphs.put(entry.getKey(), new GraphData(g.nodes(), wires, g.variables(),
                    g.placemats(), g.stickyNotes(), g.graphInterface()));
        }
        return new GraphLibrary(library.formatVersion(), library.kind(), library.main(), graphs);
    }

    // ---------- v7 → v8：ref.ac 命名变量端口撤除（AC 图化） ----------

    /**
     * v7 给 ref.ac 也加了 read:/write: 命名变量端口与 declvar- 接线；v8 撤除——AC 的变量
     * 引用由 AC 图自身承担（ac.state 的 on_entry/on_exit exec 链内 variable 节点），
     * ref.ac 回到纯引用节点（规格 nodegraph-animation-variable-refs §2.1，用户决策
     * 2026-08-08：AC 类似 RC 图化，非类似 Animation）。剥除 ref.ac 的 var_refs 快照、
     * 指向其 read:/write: 端口的连线、以及因此失去全部出边的 declvar- 节点；
     * ref.animation 的命名端口与接线保留。v7 合入的 AC 源变量声明保留（无副作用，
     * 用户可在变量表删除）。
     */
    private static GraphLibrary migrateV7ToV8(GraphLibrary library) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            GraphData g = entry.getValue();
            Set<String> acRefUids = new HashSet<>();
            for (NodeInstance n : g.nodes()) {
                if (NodeTypes.REF_AC.id().equals(n.type())) {
                    acRefUids.add(n.uid());
                }
            }
            List<Wire> wires = new ArrayList<>();
            Set<String> usedDeclvar = new HashSet<>();
            for (Wire w : g.wires()) {
                if (acRefUids.contains(w.to().node()) && NodeTypes.isVarRefPort(w.to().port())) {
                    continue;
                }
                wires.add(w);
                if (w.from().node().startsWith("declvar-")) {
                    usedDeclvar.add(w.from().node());
                }
            }
            List<NodeInstance> nodes = new ArrayList<>();
            for (NodeInstance n : g.nodes()) {
                if (n.uid().startsWith("declvar-") && !usedDeclvar.contains(n.uid())) {
                    continue;
                }
                if (acRefUids.contains(n.uid()) && n.options().containsKey(NodeTypes.VAR_REFS_OPTION)) {
                    Map<String, JsonElement> options = new LinkedHashMap<>(n.options());
                    options.remove(NodeTypes.VAR_REFS_OPTION);
                    nodes.add(new NodeInstance(n.uid(), n.type(), n.x(), n.y(), options, n.constants()));
                    continue;
                }
                nodes.add(n);
            }
            graphs.put(entry.getKey(), new GraphData(nodes, wires, g.variables(),
                    g.placemats(), g.stickyNotes(), g.graphInterface()));
        }
        return new GraphLibrary(library.formatVersion(), library.kind(), library.main(), graphs);
    }

    // ---------- v6 → v7：decl_variables 桶 → 命名变量端口 ----------

    /**
     * v6 的 decl_variables 桶接线（declvar- 节点 + decl_variables 连线）被命名变量端口
     * （read:/write: + var_refs 快照）取代（规格 nodegraph-animation-variable-refs §2.1，
     * 用户决策 2026-08-08：桶无类型、指代不明）。迁移剥除旧节点与旧连线——新接线需
     * 被引内容（动画/AC 原始文档）才能重建，迁移上下文没有，重新导入即可自动恢复。
     */
    private static GraphLibrary migrateV6ToV7(GraphLibrary library) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            GraphData g = entry.getValue();
            List<NodeInstance> nodes = new ArrayList<>();
            for (NodeInstance n : g.nodes()) {
                if (!n.uid().startsWith("declvar-")) {
                    nodes.add(n);
                }
            }
            List<Wire> wires = new ArrayList<>();
            for (Wire w : g.wires()) {
                if (!"decl_variables".equals(w.to().port()) && !w.from().node().startsWith("declvar-")) {
                    wires.add(w);
                }
            }
            graphs.put(entry.getKey(), new GraphData(nodes, wires, g.variables(),
                    g.placemats(), g.stickyNotes(), g.graphInterface()));
        }
        return new GraphLibrary(library.formatVersion(), library.kind(), library.main(), graphs);
    }

    // ---------- v1 → v2：变量节点化 ----------

    private static GraphLibrary migrateV1ToV2(GraphLibrary library) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            graphs.put(entry.getKey(), migrateGraph(entry.getValue()));
        }
        return new GraphLibrary(2, library.kind(), library.main(), graphs);
    }

    private static GraphData migrateGraph(GraphData graph) {
        List<NodeInstance> nodes = new ArrayList<>();
        List<Wire> wires = new ArrayList<>(graph.wires());
        Set<String> uidTaken = new HashSet<>();
        for (NodeInstance node : graph.nodes()) {
            uidTaken.add(node.uid());
        }
        int[] counter = {0};
        for (NodeInstance node : graph.nodes()) {
            switch (node.type()) {
                case "var.get" -> {
                    String name = optionString(node, "name", "");
                    nodes.add(new NodeInstance(node.uid(), "variable", node.x(), node.y(),
                            Map.of("name", new JsonPrimitive(stripRoot(name, "variable"))),
                            node.constants()));
                }
                case "exec.set_var" -> {
                    if (!node.options().containsKey("name")) {
                        // 新格式（target 引脚、无选项）原样保留
                        nodes.add(node);
                        break;
                    }
                    String root = optionString(node, "root", "variable");
                    String name = optionString(node, "name", "");
                    if ("temp".equals(root)) {
                        nodes.add(new NodeInstance(node.uid(), "exec.set_temp", node.x(), node.y(),
                                Map.of("name", new JsonPrimitive(name)), node.constants()));
                    } else {
                        // 新 exec.set_var（无选项）+ 自动 variable 节点连线 target
                        String varUid = freshUid(uidTaken, counter);
                        nodes.add(new NodeInstance(node.uid(), "exec.set_var", node.x(), node.y(),
                                Map.of(), node.constants()));
                        nodes.add(new NodeInstance(varUid, "variable", node.x() - 180, node.y() + 30,
                                Map.of("name", new JsonPrimitive(stripRoot(name, "variable"))), Map.of()));
                        wires.add(new Wire(new PortRef(varUid, "out"), new PortRef(node.uid(), "target")));
                    }
                }
                default -> nodes.add(node);
            }
        }
        return new GraphData(List.copyOf(nodes), List.copyOf(wires), graph.variables(),
                graph.placemats(), graph.stickyNotes(), graph.graphInterface());
    }

    // ---------- v2 → v3：声明连线化（规格 §2.4） ----------

    private static GraphLibrary migrateV2ToV3(GraphLibrary library) {
        if (library.kind() != GraphKind.CLIENT_ENTITY) {
            return library;
        }
        GraphData main = library.graphs().get(library.main());
        if (main == null) {
            return library;
        }
        Optional<NodeInstance> root = main.nodes().stream()
                .filter(n -> n.type().equals("entity.root")).findFirst();
        if (root.isEmpty()) {
            return library; // 无主图根锚点：验证器 ROOT_COUNT 已报，迁移不做猜测
        }
        String rootUid = root.get().uid();

        List<NodeInstance> mainNodes = new ArrayList<>(main.nodes());
        List<Wire> mainWires = new ArrayList<>(main.wires());
        Set<String> uidTaken = new HashSet<>();
        for (NodeInstance node : mainNodes) {
            uidTaken.add(node.uid());
        }
        int[] counter = {0};

        // 主图：无声明连线的 ref.{geometry,texture,material,animation,ac} → 补线到对应声明端口
        for (NodeInstance node : main.nodes()) {
            String port = NodeTypes.DECLARATION_PORTS.get(node.type());
            if (port == null) {
                continue;
            }
            boolean wired = mainWires.stream().anyMatch(w ->
                    w.from().node().equals(node.uid()) && w.to().node().equals(rootUid)
                            && w.to().port().equals(port));
            if (!wired) {
                mainWires.add(new Wire(new PortRef(node.uid(), "ref"), new PortRef(rootUid, port)));
            }
        }

        // 主图：未连任何 rc.condition_entry 的 ref.rc → 新建条目（置于 ref 右侧，condition 端口默认 1）
        for (NodeInstance node : main.nodes()) {
            if (!node.type().equals("ref.rc")) {
                continue;
            }
            boolean connected = mainWires.stream().anyMatch(w ->
                    w.from().node().equals(node.uid()) && w.to().port().equals("rc")
                            && main.findNode(w.to().node())
                            .map(n -> n.type().equals("rc.condition_entry")).orElse(false));
            if (connected) {
                continue;
            }
            String entryUid = freshUid(uidTaken, counter);
            mainNodes.add(new NodeInstance(entryUid, "rc.condition_entry",
                    node.x() + 240, node.y(), Map.of(), Map.of()));
            mainWires.add(new Wire(new PortRef(node.uid(), "ref"), new PortRef(entryUid, "rc")));
            mainWires.add(new Wire(new PortRef(entryUid, "entry"), new PortRef(rootUid, "render_controllers")));
        }

        // 子图：完全无连线的声明类 ref → 移到主图（uid/选项保留，置于主图空闲区）并补线
        List<NodeInstance> moved = new ArrayList<>();
        Map<String, GraphData> graphs = new LinkedHashMap<>(library.graphs());
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            if (entry.getKey().equals(library.main())) {
                continue;
            }
            GraphData graph = entry.getValue();
            Set<String> wiredUids = new HashSet<>();
            for (Wire wire : graph.wires()) {
                wiredUids.add(wire.from().node());
                wiredUids.add(wire.to().node());
            }
            List<NodeInstance> remaining = new ArrayList<>();
            boolean changed = false;
            for (NodeInstance node : graph.nodes()) {
                if (NodeTypes.DECLARATION_PORTS.containsKey(node.type()) && !wiredUids.contains(node.uid())) {
                    moved.add(node);
                    changed = true;
                } else {
                    remaining.add(node);
                }
            }
            if (changed) {
                graphs.put(entry.getKey(), new GraphData(List.copyOf(remaining), graph.wires(),
                        graph.variables(), graph.placemats(), graph.stickyNotes(), graph.graphInterface()));
            }
        }
        if (!moved.isEmpty()) {
            // 主图空闲区：现有节点最下方起，向左对齐主图最小 x，纵排
            float minX = (float) mainNodes.stream().mapToDouble(NodeInstance::x).min().orElse(0);
            float y = (float) mainNodes.stream().mapToDouble(NodeInstance::y).max().orElse(0) + 160;
            for (NodeInstance node : moved) {
                String uid = uidTaken.contains(node.uid()) ? freshUid(uidTaken, counter) : node.uid();
                uidTaken.add(uid);
                mainNodes.add(new NodeInstance(uid, node.type(), minX, y,
                        node.options(), node.constants()));
                mainWires.add(new Wire(new PortRef(uid, "ref"),
                        // moved 由上方 DECLARATION_PORTS.containsKey 过滤，get 必中
                        new PortRef(rootUid, Objects.requireNonNull(NodeTypes.DECLARATION_PORTS.get(node.type())))));
                y += 140;
            }
        }

        graphs.put(library.main(), new GraphData(List.copyOf(mainNodes), List.copyOf(mainWires),
                main.variables(), main.placemats(), main.stickyNotes(), main.graphInterface()));
        return new GraphLibrary(3, library.kind(), library.main(), graphs);
    }

    // ---------- v3 → v4：RenderController 内联（规格 nodegraph-inline-render-controller §5） ----------

    /**
     * v3 → v4（仅 CLIENT_ENTITY 库）：
     * <ul>
     *   <li>rc.condition_entry 拆解：rc 线源（ref.rc）的 ref 直连 entity.root.render_controllers；
     *       condition 线/内联值移到该 ref.rc 的 condition 端口（同 ref 多条 entry 先者胜）；删 entry；</li>
     *   <li>entity.root geometries/textures/materials 上的声明线 → 重定向到主图第一个 ref.rc
     *       （uid 序）的同名声明端口；无 ref.rc → 断线（验证器 REF_NOT_CONNECTED 提示）；</li>
     *   <li>RENDER_CONTROLLER / ANIMATION_CONTROLLER 库不变（rc.root 新端口闲置）。</li>
     * </ul>
     */
    private static GraphLibrary migrateV3ToV4(GraphLibrary library) {
        if (library.kind() != GraphKind.CLIENT_ENTITY) {
            return new GraphLibrary(4, library.kind(), library.main(), library.graphs());
        }
        GraphData main = library.graphs().get(library.main());
        if (main == null) {
            return new GraphLibrary(4, library.kind(), library.main(), library.graphs());
        }
        Optional<NodeInstance> root = main.nodes().stream()
                .filter(n -> n.type().equals("entity.root")).findFirst();
        if (root.isEmpty()) {
            return new GraphLibrary(4, library.kind(), library.main(), library.graphs());
        }
        String rootUid = root.get().uid();

        List<NodeInstance> nodes = new ArrayList<>(main.nodes());
        List<Wire> wires = new ArrayList<>(main.wires());

        // 1. rc.condition_entry 拆解
        for (NodeInstance entry : main.nodes()) {
            if (!entry.type().equals("rc.condition_entry")) {
                continue;
            }
            // rc 线源
            String rcUid = null;
            for (Wire w : wires) {
                if (w.to().node().equals(entry.uid()) && w.to().port().equals("rc")) {
                    rcUid = w.from().node();
                    break;
                }
            }
            if (rcUid != null) {
                String rc = rcUid;
                boolean alreadyMounted = wires.stream().anyMatch(w ->
                        w.from().node().equals(rc) && w.from().port().equals("ref")
                                && w.to().node().equals(rootUid) && w.to().port().equals("render_controllers"));
                if (!alreadyMounted) {
                    wires.add(new Wire(new PortRef(rc, "ref"), new PortRef(rootUid, "render_controllers")));
                }
                // condition：线或内联值迁移（ref.rc 已有 condition 内容时先者胜）
                boolean refHasCondition = wires.stream().anyMatch(w ->
                        w.to().node().equals(rc) && w.to().port().equals("condition"));
                if (!refHasCondition) {
                    Optional<Wire> condWire = wires.stream().filter(w ->
                            w.to().node().equals(entry.uid()) && w.to().port().equals("condition")).findFirst();
                    if (condWire.isPresent()) {
                        wires.add(new Wire(condWire.get().from(), new PortRef(rc, "condition")));
                    } else if (entry.constants().containsKey("condition")) {
                        // 内联常量搬到 ref.rc（替换节点）
                        for (int i = 0; i < nodes.size(); i++) {
                            NodeInstance n = nodes.get(i);
                            if (n.uid().equals(rc) && !n.constants().containsKey("condition")) {
                                Map<String, JsonElement> constants = new LinkedHashMap<>(n.constants());
                                constants.put("condition", entry.constants().get("condition"));
                                nodes.set(i, new NodeInstance(n.uid(), n.type(), n.x(), n.y(),
                                        n.options(), Map.copyOf(constants)));
                            }
                        }
                    }
                }
            }
            // 删 entry 及其全部线
            nodes.removeIf(n -> n.uid().equals(entry.uid()));
            wires.removeIf(w -> w.from().node().equals(entry.uid()) || w.to().node().equals(entry.uid()));
        }

        // 2. entity.root 的 geo/tex/mat 声明线 → 重定向第一个 ref.rc
        String firstRc = nodes.stream().filter(n -> n.type().equals("ref.rc"))
                .map(NodeInstance::uid).min(String::compareTo).orElse(null);
        Set<String> legacyPorts = Set.of("geometries", "textures", "materials");
        List<Wire> retargeted = new ArrayList<>();
        for (Wire w : wires) {
            if (w.to().node().equals(rootUid) && legacyPorts.contains(w.to().port())) {
                if (firstRc != null) {
                    retargeted.add(new Wire(w.from(), new PortRef(firstRc, "decl_" + w.to().port())));
                } // 无 ref.rc → 断线
            } else {
                retargeted.add(w);
            }
        }
        wires = retargeted;

        Map<String, GraphData> graphs = new LinkedHashMap<>(library.graphs());
        graphs.put(library.main(), new GraphData(List.copyOf(nodes), List.copyOf(wires),
                main.variables(), main.placemats(), main.stickyNotes(), main.graphInterface()));
        return new GraphLibrary(4, library.kind(), library.main(), graphs);
    }

    /**
     * v4 → v5（规格 §6.5）：rc.root 16 个 float 颜色通道端口 → 4 个 COLOR 端口。
     *
     * <ul>
     *   <li>四通道均无无线无常数 → 不迁移该字段（新端口留空 = 字段不输出，同语义）；</li>
     *   <li>四通道均无连线且常数（缺省 1）全部 8bit 精确 → const.color（取色器节点）接线；</li>
     *   <li>否则 → color.compose：通道线/内联常数原样搬到 r/g/b/a，compose 输出接颜色端口；</li>
     *   <li>所有库类型都迁移（rc.root 存在于 RENDER_CONTROLLER 与 CLIENT_ENTITY 库）。</li>
     * </ul>
     */
    private static GraphLibrary migrateV4ToV5(GraphLibrary library) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            graphs.put(entry.getKey(), migrateColorsV5(entry.getValue()));
        }
        return new GraphLibrary(5, library.kind(), library.main(), graphs);
    }

    /** (字段端口, 通道前缀) 四组颜色。 */
    private static final String[][] COLOR_FIELDS = {
            {"color", "color"}, {"is_hurt_color", "is_hurt"},
            {"on_fire_color", "on_fire"}, {"overlay_color", "overlay"}};
    private static final String[] CHANNELS = {"r", "g", "b", "a"};

    private static GraphData migrateColorsV5(GraphData graph) {
        List<NodeInstance> nodes = new ArrayList<>(graph.nodes());
        List<Wire> wires = new ArrayList<>(graph.wires());
        Set<String> uidTaken = new HashSet<>();
        for (NodeInstance n : nodes) {
            uidTaken.add(n.uid());
        }
        int[] counter = {0};

        for (int i = 0; i < nodes.size(); i++) {
            NodeInstance rc = nodes.get(i);
            if (!rc.type().equals("rc.root")) {
                continue;
            }
            Map<String, JsonElement> rcConstants = new LinkedHashMap<>(rc.constants());
            int created = 0;
            for (String[] field : COLOR_FIELDS) {
                String fieldPort = field[0];
                String[] channelPorts = new String[4];
                for (int c = 0; c < 4; c++) {
                    channelPorts[c] = field[1] + "_" + CHANNELS[c];
                }
                // 收集每通道的线与内联常数
                Wire[] channelWires = new Wire[4];
                JsonElement[] channelConstants = new JsonElement[4];
                boolean anyContent = false;
                boolean anyWire = false;
                boolean allByteExact = true;
                for (int c = 0; c < 4; c++) {
                    String cp = channelPorts[c];
                    int idx = c;
                    channelWires[c] = wires.stream()
                            .filter(w -> w.to().node().equals(rc.uid()) && w.to().port().equals(cp))
                            .findFirst().orElse(null);
                    channelConstants[c] = rcConstants.get(cp);
                    if (channelWires[idx] != null) {
                        anyWire = true;
                        anyContent = true;
                    }
                    if (channelConstants[idx] != null) {
                        anyContent = true;
                        JsonElement cc = channelConstants[idx];
                        if (!(cc instanceof JsonPrimitive p) || !p.isNumber()
                                || !ColorValues.isByteExact(p.getAsDouble())) {
                            allByteExact = false;
                        }
                    }
                }
                if (!anyContent) {
                    continue;
                }
                // 创建 const.color / color.compose
                String newUid = freshUid(uidTaken, counter);
                uidTaken.add(newUid);
                NodeInstance created_;
                if (!anyWire && allByteExact) {
                    float[] values = new float[4];
                    for (int c = 0; c < 4; c++) {
                        values[c] = channelConstants[c] != null
                                ? (float) channelConstants[c].getAsDouble() : 1f;
                    }
                    created_ = new NodeInstance(newUid, "const.color",
                            rc.x() + 60, rc.y() + 40 * (created++),
                            Map.of("value", new JsonPrimitive(
                                    ColorValues.toHex(values[0], values[1], values[2], values[3]))),
                            Map.of());
                } else {
                    Map<String, JsonElement> composeConstants = new LinkedHashMap<>();
                    for (int c = 0; c < 4; c++) {
                        if (channelConstants[c] != null) {
                            composeConstants.put(CHANNELS[c], channelConstants[c]);
                        }
                    }
                    created_ = new NodeInstance(newUid, "color.compose",
                            rc.x() + 60, rc.y() + 40 * (created++),
                            Map.of(), Map.copyOf(composeConstants));
                    for (int c = 0; c < 4; c++) {
                        if (channelWires[c] != null) {
                            wires.add(new Wire(channelWires[c].from(), new PortRef(newUid, CHANNELS[c])));
                        }
                    }
                }
                nodes.add(created_);
                wires.add(new Wire(new PortRef(newUid, "out"), new PortRef(rc.uid(), fieldPort)));
                // 拆旧：通道线与通道常数
                for (String cp : channelPorts) {
                    wires.removeIf(w -> w.to().node().equals(rc.uid()) && w.to().port().equals(cp));
                    rcConstants.remove(cp);
                }
            }
            if (!rcConstants.equals(rc.constants())) {
                nodes.set(i, new NodeInstance(rc.uid(), rc.type(), rc.x(), rc.y(),
                        rc.options(), Map.copyOf(rcConstants)));
            }
        }
        return new GraphData(List.copyOf(nodes), List.copyOf(wires),
                graph.variables(), graph.placemats(), graph.stickyNotes(), graph.graphInterface());
    }

    private static String optionString(NodeInstance node, String id, String fallback) {
        JsonElement value = node.options().get(id);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : fallback;
    }

    // ---------- v5 → v6：有序条目链 + rc.root decl_* 移除 ----------

    private static final Set<String> V6_LIST_PORTS = Set.of("textures", "materials", "part_visibility");
    private static final Set<String> V6_DECL_PORTS = Set.of(
            "decl_geometries", "decl_textures", "decl_materials");

    private static GraphLibrary migrateV5ToV6(GraphLibrary library) {
        if (library.kind() != GraphKind.CLIENT_ENTITY && library.kind() != GraphKind.RENDER_CONTROLLER) {
            return new GraphLibrary(6, library.kind(), library.main(), library.graphs());
        }
        GraphData main = library.graphs().get(library.main());
        if (main == null) {
            return new GraphLibrary(6, library.kind(), library.main(), library.graphs());
        }
        Set<String> rcRoots = new HashSet<>();
        for (NodeInstance n : main.nodes()) {
            if (n.type().equals("rc.root")) {
                rcRoots.add(n.uid());
            }
        }
        if (rcRoots.isEmpty()) {
            return new GraphLibrary(6, library.kind(), library.main(), library.graphs());
        }
        List<Wire> wires = new ArrayList<>(main.wires());
        // decl_* 连线删除（ref 节点保留；协议短名由 DeclarationTables carve-out 接管）
        wires.removeIf(w -> rcRoots.contains(w.to().node()) && V6_DECL_PORTS.contains(w.to().port()));
        // 多条目线 → 链（uid 序 = v5 发射序，产物等价）
        for (String rcUid : rcRoots.stream().sorted().toList()) {
            for (String port : V6_LIST_PORTS) {
                List<Wire> direct = wires.stream()
                        .filter(w -> w.to().node().equals(rcUid) && w.to().port().equals(port))
                        .sorted((a, b) -> a.from().node().compareTo(b.from().node()))
                        .toList();
                if (direct.size() <= 1) {
                    continue;
                }
                wires.removeAll(direct);
                wires.add(direct.get(0));
                for (int i = 1; i < direct.size(); i++) {
                    wires.add(new Wire(direct.get(i).from(),
                            new PortRef(direct.get(i - 1).from().node(), "next")));
                }
            }
        }
        Map<String, GraphData> graphs = new LinkedHashMap<>(library.graphs());
        graphs.put(library.main(), new GraphData(main.nodes(), List.copyOf(wires),
                main.variables(), main.placemats(), main.stickyNotes(), main.graphInterface()));
        return new GraphLibrary(6, library.kind(), library.main(), graphs);
    }

    private static String stripRoot(String name, String root) {
        String prefix = root + ".";
        return name.startsWith(prefix) ? name.substring(prefix.length()) : name;
    }

    private static String freshUid(Set<String> taken, int[] counter) {
        String uid;
        do {
            uid = "mig" + counter[0]++;
        } while (taken.contains(uid));
        taken.add(uid);
        return uid;
    }
}
