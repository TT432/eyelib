package io.github.tt432.eyelib.nodegraph.codegen;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.nodegraph.ColorValues;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.EmolangFunction;
import io.github.tt432.eyelib.nodegraph.EmolangRegistry;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphInterface;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.GraphVariableOps;
import io.github.tt432.eyelib.nodegraph.MolangLiterals;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortDef;
import io.github.tt432.eyelib.nodegraph.PortDirection;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.ShortNames;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 单次槽生成的会话（每个 emit 调用一个实例，保证 §2.4-2 确定性）。
 *
 * <p>入口两类：值/EXEC 汇入槽（IN 端口，如 scale、loop.body、subgraph.output.exec_in）
 * 与 EXEC 时机源端口（OUT 端口，v13 起：event.* 节点的 exec_out、ac.state 的
 * on_entry/on_exit；规格 nodegraph-event-nodes）。链拓扑不变（exec_out → exec_in），
 * 只是发射起点从「槽口反向走」变为「源端口正向走」。
 *
 * <p>两阶段：
 * <ol>
 *   <li><b>计数</b>：从槽口沿数据流反向遍历，统计每个值节点在本槽 DAG 内的使用次数
 *       （出度），并递归为每个 subgraph.call 建立子帧（递归/深度检测在此报告）；</li>
 *   <li><b>发射</b>：深度优先发射；出度 ≥ 2 的非平凡节点先产出
 *       {@code temp.g<N> = <expr>;} 前置语句，引用处替换为 temp 名（§2.4-3）。</li>
 * </ol>
 *
 * <p>子图展开（D5/§2.4-9）：实参在调用点（父帧）先发射；子图内 subgraph.input 锚点引用
 * 替换为实参表达式；形参被引用 ≥ 2 次且非平凡时提取 {@code temp.sg<K>_<param>}；
 * 子图内提取的 temp 统一加 {@code sg<K>_} 前缀（K = 全局调用计数）。
 */
final class EmitSession {
    private final GraphLibrary library;
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    /** .emolang 内联展开器（展开栈与调用计数随会话隔离）。 */
    private final EmolangExpander emolangExpander = new EmolangExpander(diagnostics);
    /** 计数阶段（出度统计 + 子图预展开）。 */
    private final SlotUseCounter useCounter;

    EmitSession(GraphLibrary library) {
        this.library = library;
        this.useCounter = new SlotUseCounter(library, diagnostics);
    }

    // ---------- 入口 ----------

    CodegenResult emitExpression(String graphName, PortRef slot) {
        Optional<Frame> frame = openSlot(graphName, slot);
        if (frame.isEmpty()) {
            return new CodegenResult("0", diagnostics);
        }
        Frame f = frame.get();
        useCounter.countValueInput(f, slot.node(), slot.port());
        Out out = emitValueInput(f, slot.node(), slot.port());
        List<String> parts = new ArrayList<>(out.preludes());
        parts.add(out.expr());
        return new CodegenResult(String.join("; ", parts), diagnostics);
    }

    /**
     * 从 EXEC OUT 时机源端口正向发射语句序列（v13，规格 nodegraph-event-nodes）：
     * 源端口（event.* 节点的 exec_out、ac.state 的 on_entry/on_exit）连链首，
     * 沿 exec_out 正向走到链尾。空链 → "0"。
     */
    CodegenResult emitStatementListFrom(String graphName, PortRef source) {
        Optional<Frame> frame = openSource(graphName, source);
        if (frame.isEmpty()) {
            return new CodegenResult("0", diagnostics);
        }
        Frame f = frame.get();
        useCounter.countExecFrom(f, source.node(), source.port(), true);
        List<String> statements = new ArrayList<>();
        for (NodeInstance node : ExecChain.forward(f.graph, source.node(), source.port(), true, diagnostics)) {
            statements.addAll(emitStatement(f, node));
        }
        String code = statements.isEmpty() ? "0" : String.join("; ", statements);
        return new CodegenResult(code, diagnostics);
    }

    /** 校验 EXEC OUT 时机源端口并建立根帧；失败时报告诊断并返回空。 */
    private Optional<Frame> openSource(String graphName, PortRef source) {
        Optional<GraphData> graph = library.graph(graphName);
        if (graph.isEmpty()) {
            error("UNKNOWN_GRAPH", "graph '" + graphName + "' not found in library");
            return Optional.empty();
        }
        Optional<NodeInstance> node = graph.get().findNode(source.node());
        if (node.isEmpty()) {
            error("UNKNOWN_NODE", "node '" + source.node() + "' not found in graph '" + graphName + "'",
                    source.node());
            return Optional.empty();
        }
        Optional<NodeType> type = NodeTypes.get(node.get().type());
        if (type.isEmpty()) {
            error("UNKNOWN_NODE_TYPE", "unknown node type '" + node.get().type() + "'", source.node());
            return Optional.empty();
        }
        Frame frame = new Frame(library, graph.get(), "");
        Optional<PortDef> port = type.get().outputsOf(node.get(), frame.resolver).stream()
                .filter(p -> p.id().equals(source.port()) && p.direction() == PortDirection.OUT
                        && p.type() == PortType.EXEC)
                .findFirst();
        if (port.isEmpty()) {
            error("INVALID_SLOT",
                    "port '" + source.port() + "' of node '" + source.node()
                            + "' is not an exec output source port", source.node());
            return Optional.empty();
        }
        return Optional.of(frame);
    }

    /** 校验图/节点/值输入槽并建立根帧；失败时报告诊断并返回空。 */
    private Optional<Frame> openSlot(String graphName, PortRef slot) {
        Optional<GraphData> graph = library.graph(graphName);
        if (graph.isEmpty()) {
            error("UNKNOWN_GRAPH", "graph '" + graphName + "' not found in library");
            return Optional.empty();
        }
        Optional<NodeInstance> node = graph.get().findNode(slot.node());
        if (node.isEmpty()) {
            error("UNKNOWN_NODE", "node '" + slot.node() + "' not found in graph '" + graphName + "'", slot.node());
            return Optional.empty();
        }
        Optional<NodeType> type = NodeTypes.get(node.get().type());
        if (type.isEmpty()) {
            error("UNKNOWN_NODE_TYPE", "unknown node type '" + node.get().type() + "'", slot.node());
            return Optional.empty();
        }
        Frame frame = new Frame(library, graph.get(), "");
        Optional<PortDef> port = type.get().inputsOf(node.get(), frame.resolver).stream()
                .filter(p -> p.id().equals(slot.port()) && p.direction() == PortDirection.IN)
                .findFirst();
        boolean ok = port.isPresent() && port.get().type().isValue();
        if (!ok) {
            error("INVALID_SLOT",
                    "port '" + slot.port() + "' of node '" + slot.node() + "' is not a value input slot",
                    slot.node());
            return Optional.empty();
        }
        return Optional.of(frame);
    }

    // ---------- 值发射 ----------

    /** 发射某节点的值输入端口：已连线 → 发射生产者；否则 constants → 端口默认 → UNCONNECTED_INPUT。 */
    private Out emitValueInput(Frame f, String consumerUid, String portId) {
        Optional<Wire> wire = EmitSupport.wireInto(f.graph, consumerUid, portId);
        if (wire.isPresent()) {
            Optional<NodeInstance> producer = f.graph.findNode(wire.get().from().node());
            if (producer.isPresent()) {
                return emitValueProducer(f, producer.get(), wire.get().from().port());
            }
            error("UNKNOWN_NODE", "wire source node '" + wire.get().from().node() + "' not found", consumerUid);
            return Out.of("0");
        }
        Optional<NodeInstance> consumer = f.graph.findNode(consumerUid);
        if (consumer.isPresent()) {
            JsonElement inline = consumer.get().constants().get(portId);
            if (inline != null) {
                return Out.of(literal(inline, consumerUid));
            }
            Optional<NodeType> type = NodeTypes.get(consumer.get().type());
            if (type.isPresent()) {
                Optional<PortDef> port = type.get().inputsOf(consumer.get(), f.resolver).stream()
                        .filter(p -> p.id().equals(portId)).findFirst();
                if (port.isPresent() && port.get().defaultValue().isPresent()) {
                    return Out.of(literal(port.get().defaultValue().get(), consumerUid));
                }
            }
        }
        error("UNCONNECTED_INPUT",
                "input '" + portId + "' of node '" + consumerUid + "' is unconnected and has no default",
                consumerUid);
        return Out.of("0");
    }

    /**
     * 发射某节点<b>值输出端口</b>的产出表达式（画布调试徽标用；规格 nodegraph-workbench §W3）。
     * 与 {@link #emitExpression} 同会话语义（temp.gN 提取/子图内联），仅入口从输入槽换成生产者输出。
     */
    CodegenResult emitNodeOutput(String graphName, String nodeUid, String portId) {
        Optional<GraphData> graph = library.graph(graphName);
        if (graph.isEmpty()) {
            error("UNKNOWN_GRAPH", "graph '" + graphName + "' not found in library");
            return new CodegenResult("0", diagnostics);
        }
        Optional<NodeInstance> node = graph.get().findNode(nodeUid);
        if (node.isEmpty()) {
            error("UNKNOWN_NODE", "node '" + nodeUid + "' not found in graph '" + graphName + "'", nodeUid);
            return new CodegenResult("0", diagnostics);
        }
        Optional<NodeType> type = NodeTypes.get(node.get().type());
        if (type.isEmpty()) {
            error("UNKNOWN_NODE_TYPE", "unknown node type '" + node.get().type() + "'", nodeUid);
            return new CodegenResult("0", diagnostics);
        }
        Frame f = new Frame(library, graph.get(), "");
        boolean valid = type.get().outputsOf(node.get(), f.resolver).stream()
                .anyMatch(p -> p.id().equals(portId) && p.type().isValue());
        if (!valid) {
            error("INVALID_SLOT", "port '" + portId + "' of node '" + nodeUid + "' is not a value output port", nodeUid);
            return new CodegenResult("0", diagnostics);
        }
        useCounter.countNode(f, node.get(), type.get());
        Out out = emitValueProducer(f, node.get(), portId);
        List<String> parts = new ArrayList<>(out.preludes());
        parts.add(out.expr());
        return new CodegenResult(String.join("; ", parts), diagnostics);
    }

    // ---------- 颜色发射 ----------

    /**
     * 发射颜色端口（COLOR 类型 IN 端口）：已连线 → 四通道 {@link ColorCode}；未连线 → code=null
     * （调用方省略该颜色字段）。四通道各自独立会话（不共享 temp 提取——JSON 中四通道是四个
     * 独立 ExprSet 字符串）。
     */
    ColorCodegenResult emitColor(String graphName, PortRef slot) {
        Optional<GraphData> graph = library.graph(graphName);
        if (graph.isEmpty()) {
            error("UNKNOWN_GRAPH", "graph '" + graphName + "' not found in library");
            return new ColorCodegenResult(null, diagnostics);
        }
        Optional<NodeInstance> node = graph.get().findNode(slot.node());
        if (node.isEmpty()) {
            error("UNKNOWN_NODE", "node '" + slot.node() + "' not found in graph '" + graphName + "'", slot.node());
            return new ColorCodegenResult(null, diagnostics);
        }
        Optional<Wire> wire = EmitSupport.wireInto(graph.get(), slot.node(), slot.port());
        if (wire.isEmpty()) {
            return new ColorCodegenResult(null, diagnostics);
        }
        Optional<NodeInstance> producer = graph.get().findNode(wire.get().from().node());
        if (producer.isEmpty()) {
            error("UNKNOWN_NODE", "wire source node '" + wire.get().from().node() + "' not found", slot.node());
            return new ColorCodegenResult(null, diagnostics);
        }
        Optional<NodeType> type = NodeTypes.get(producer.get().type());
        if (type.isEmpty()) {
            error("UNKNOWN_NODE_TYPE", "unknown node type '" + producer.get().type() + "'", producer.get().uid());
            return new ColorCodegenResult(null, diagnostics);
        }
        return switch (type.get().kind()) {
            case CONST_COLOR -> {
                float[] c = ColorValues.parse(EmitSupport.string(producer.get(), type.get(), "value"));
                if (c == null) {
                    error("INVALID_COLOR_VALUE", "const.color 的 value 不是 #RRGGBB/#AARRGGBB，按白色处理",
                            producer.get().uid());
                    c = new float[]{1, 1, 1, 1};
                }
                yield new ColorCodegenResult(new ColorCode(
                        EmitSupport.formatNumber(c[0]), EmitSupport.formatNumber(c[1]),
                        EmitSupport.formatNumber(c[2]), EmitSupport.formatNumber(c[3])),
                        diagnostics);
            }
            case COLOR_COMPOSE -> new ColorCodegenResult(new ColorCode(
                    emitColorChannel(graph.get(), producer.get(), "r"),
                    emitColorChannel(graph.get(), producer.get(), "g"),
                    emitColorChannel(graph.get(), producer.get(), "b"),
                    emitColorChannel(graph.get(), producer.get(), "a")),
                    diagnostics);
            default -> {
                error("INVALID_COLOR_SOURCE",
                        "颜色端口只能接 const.color / color.compose（实际 '" + producer.get().type() + "'）",
                        producer.get().uid());
                yield new ColorCodegenResult(null, diagnostics);
            }
        };
    }

    /** color.compose 单通道发射：独立帧（通道间不共享 temp 提取）。 */
    private String emitColorChannel(GraphData graph, NodeInstance compose, String channel) {
        Frame f = new Frame(library, graph, "");
        useCounter.countValueInput(f, compose.uid(), channel);
        Out out = emitValueInput(f, compose.uid(), channel);
        List<String> parts = new ArrayList<>(out.preludes());
        parts.add(out.expr());
        return String.join("; ", parts);
    }

    /** 发射生产者节点的输出；出度 ≥ 2 的非平凡节点提取 temp.gN（§2.4-3）。 */
    private Out emitValueProducer(Frame f, NodeInstance node, String portId) {
        Optional<NodeType> type = NodeTypes.get(node.type());
        if (type.isPresent() && type.get().kind() == NodeType.Kind.SUBGRAPH_INPUT) {
            String bound = f.bindings.get(portId);
            if (bound != null) {
                return Out.of(bound);
            }
            error("UNCONNECTED_INPUT", "subgraph input '" + portId + "' has no bound argument", node.uid());
            return Out.of("0");
        }
        String extracted = f.extracted.get(node.uid());
        if (extracted != null) {
            return Out.of(extracted);
        }
        Out out = emitValueNode(f, node, type);
        int uses = f.useCounts.getOrDefault(node.uid(), 0);
        if (uses >= 2 && type.isPresent() && !EmitSupport.isTrivial(type.get(), node)) {
            String name = "temp." + f.tempPrefix + "g" + f.tempCounter++;
            f.extracted.put(node.uid(), name);
            List<String> preludes = new ArrayList<>(out.preludes());
            preludes.add(name + " = " + out.expr());
            return new Out(preludes, name);
        }
        return out;
    }

    private Out emitValueNode(Frame f, NodeInstance node, Optional<NodeType> typeOpt) {
        if (typeOpt.isEmpty()) {
            error("UNKNOWN_NODE_TYPE", "unknown node type '" + node.type() + "'", node.uid());
            return Out.of("0");
        }
        NodeType type = typeOpt.get();
        return switch (type.kind()) {
            case CONST_NUMBER, CONST_INT -> Out.of(EmitSupport.number(node, type, "value"));
            case CONST_BOOL -> Out.of(EmitSupport.bool(node, type, "value") ? "1" : "0");
            case CONST_STRING -> Out.of(EmitSupport.quote(EmitSupport.string(node, type, "value")));
            case CONST_COLOR, COLOR_COMPOSE -> {
                // 颜色是复合值，只能经 emitColor 进入颜色端口；流到标量上下文即图有误
                error("COLOR_AS_SCALAR", "color value cannot be used as a scalar molang expression", node.uid());
                yield Out.of("0");
            }
            // 变量根按黑板声明的 molang 作用域选择（规格 nodegraph-variable-table §2.2）；
            // 未声明回落 variable.*（验证器另有 UNDECLARED_VARIABLE 警告）
            case VARIABLE -> {
                String varName = EmitSupport.string(node, type, "name");
                String bare = varName.startsWith("variable.") || varName.startsWith("temp.")
                        ? varName.substring(varName.indexOf('.') + 1) : varName;
                boolean temp = GraphVariableOps.find(f.graph, bare)
                        .map(d -> d.scope() == VariableDecl.Scope.TEMP)
                        .orElse(false);
                yield Out.of(EmitSupport.withRoot(temp ? "temp" : "variable", varName));
            }
            case TEMP_GET -> Out.of(EmitSupport.withRoot("temp", EmitSupport.string(node, type, "name")));
            case CONTEXT_GET -> Out.of(EmitSupport.withRoot("context", EmitSupport.string(node, type, "name")));
            case QUERY_CALL, MATH_CALL -> emitCallLike(f, node, type);
            case OP_BINARY -> {
                Out a = emitValueInput(f, node.uid(), "a");
                Out b = emitValueInput(f, node.uid(), "b");
                yield EmitSupport.combine(List.of(a, b),
                        "(" + a.expr() + " " + NodeTypes.opSymbolOf(type.id()) + " " + b.expr() + ")");
            }
            case OP_UNARY -> {
                Out a = emitValueInput(f, node.uid(), "a");
                yield EmitSupport.combine(List.of(a), "(" + NodeTypes.opSymbolOf(type.id()) + a.expr() + ")");
            }
            case OP_TERNARY -> {
                Out cond = emitValueInput(f, node.uid(), "cond");
                Out a = emitValueInput(f, node.uid(), "a");
                Out b = emitValueInput(f, node.uid(), "b");
                yield EmitSupport.combine(List.of(cond, a, b),
                        "(" + cond.expr() + " ? " + a.expr() + " : " + b.expr() + ")");
            }
            case OP_NULLCOALESCE -> {
                Out a = emitValueInput(f, node.uid(), "a");
                Out b = emitValueInput(f, node.uid(), "b");
                yield EmitSupport.combine(List.of(a, b), "(" + a.expr() + " ?? " + b.expr() + ")");
            }
            case REF_GEOMETRY -> Out.of("geometry." + ShortNames.effective(node, type));
            case REF_TEXTURE -> Out.of("texture." + ShortNames.effective(node, type));
            case REF_MATERIAL -> Out.of("material." + ShortNames.effective(node, type));
            case SUBGRAPH_CALL -> emitSubgraphCall(f, node, type);
            default -> {
                error("UNSUPPORTED_NODE",
                        "node type '" + type.id() + "' cannot appear in value context", node.uid());
                yield Out.of("0");
            }
        };
    }

    /** query.call / math.call / exec.call 的公共发射：0 参数 → 属性访问形，否则调用形。 */
    private Out emitCallLike(Frame f, NodeInstance node, NodeType type) {
        String function = EmitSupport.string(node, type, "function");
        // .emolang 自定义函数 → 内联展开（规格 nodegraph-emolang-functions §4）
        EmolangFunction custom = EmolangRegistry.find(function);
        if (custom != null) {
            return emitEmolangCall(f, node, type, custom);
        }
        // 裸名且非顶层内建（loop/for_each 等）→ 直发会产生非法 molang，提前警告
        if (!function.isBlank() && function.indexOf('.') < 0
                && !io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries
                        .mappingTree().toplevelNode.actualFunctions.containsKey(function)) {
            diagnostics.add(Diagnostic.warning("UNKNOWN_FUNCTION",
                    "unknown bare function '" + function
                            + "' (not a rooted built-in, not a loaded .emolang); emitted as-is",
                    node.uid()));
        }
        List<String> preludes = new ArrayList<>();
        List<String> args = new ArrayList<>();
        for (PortDef in : type.inputsOf(node, f.resolver)) {
            if (in.direction() != PortDirection.IN || !in.type().isValue()) continue;
            Out arg = emitValueInput(f, node.uid(), in.id());
            preludes.addAll(arg.preludes());
            args.add(arg.expr());
        }
        // v11：变长尾参/未知函数参数走 args 字面值列表选项（规格 nodegraph-variadic-call-list）
        args.addAll(EmitSupport.listArgLiterals(node));
        String expr = args.isEmpty() ? function : function + "(" + String.join(", ", args) + ")";
        return new Out(preludes, expr);
    }

    // ---------- .emolang 展开（规格 nodegraph-emolang-functions §4） ----------

    /** 自定义函数调用：实参在调用点帧先发射，再递归展开函数体。 */
    private Out emitEmolangCall(Frame f, NodeInstance node, NodeType type, EmolangFunction fn) {
        List<String> preludes = new ArrayList<>();
        List<String> args = new ArrayList<>();
        int missing = 0;
        for (PortDef in : type.inputsOf(node, f.resolver)) {
            if (in.direction() != PortDirection.IN || !in.type().isValue()) continue;
            boolean wired = EmitSupport.wireInto(f.graph, node.uid(), in.id()).isPresent();
            boolean inlined = node.constants().containsKey(in.id());
            if (!wired && !inlined) {
                // 未连接的形参按 0 占位（warning 而非 UNCONNECTED_INPUT 硬错误）
                missing++;
                args.add("0");
                continue;
            }
            Out arg = emitValueInput(f, node.uid(), in.id());
            preludes.addAll(arg.preludes());
            args.add(arg.expr());
        }
        List<EmolangFunction.Param> params = fn.params();
        if (missing > 0 || args.size() != params.size()) {
            diagnostics.add(Diagnostic.warning("EMOLANG_ARITY",
                    "emolang '" + fn.name() + "' takes " + params.size() + " args, got "
                            + (args.size() - missing) + " connected (missing padded with 0, extra dropped)",
                    node.uid()));
            while (args.size() < params.size()) {
                args.add("0");
            }
            if (args.size() > params.size()) {
                args = new ArrayList<>(args.subList(0, params.size()));
            }
        }
        Out expanded = emolangExpander.expand(fn, args, node.uid());
        preludes.addAll(expanded.preludes());
        return new Out(preludes, expanded.expr());
    }

    /** 子图内联展开（D5/§2.4-9）：形参替换 + temp.sg<K>_ 名称隔离。 */
    private Out emitSubgraphCall(Frame f, NodeInstance node, NodeType type) {
        Frame child = f.childFrames.get(node.uid());
        if (child == null) {
            // 诊断已在计数阶段报告（未知子图/递归/缺锚点）
            return Out.of("0");
        }
        // 计数阶段已保证子图接口存在
        GraphInterface iface = child.graph.graphInterface().orElseThrow();
        List<String> preludes = new ArrayList<>();
        Map<String, String> bindings = new HashMap<>();
        for (GraphInterface.Param param : iface.inputs()) {
            // 实参在调用点（父帧）先发射
            Out arg = emitValueInput(f, node.uid(), param.name());
            preludes.addAll(arg.preludes());
            int refs = child.paramRefs.getOrDefault(param.name(), 0);
            String bound;
            if (refs >= 2 && !EmitSupport.isDuplicatable(arg.expr())) {
                bound = "temp." + child.tempPrefix + param.name();
                preludes.add(bound + " = " + arg.expr());
            } else {
                bound = arg.expr();
            }
            bindings.put(param.name(), bound);
        }
        child.bindings = bindings;
        if (child.outputAnchor.isEmpty()) {
            return new Out(preludes, "0");
        }
        NodeInstance anchor = child.outputAnchor.get();
        // exec_in 链先于 result 发射（§2.4：语句; 结果 的 ExprSet）
        preludes.addAll(emitExecInput(child, anchor.uid(), "exec_in"));
        Out result = emitValueInput(child, anchor.uid(), "result");
        preludes.addAll(result.preludes());
        return new Out(preludes, result.expr());
    }

    // ---------- 执行链发射 ----------

    /** 发射 EXEC 槽：槽口连的是链尾，沿 exec_in 反向走到链首后正向发射。 */
    private List<String> emitExecInput(Frame f, String consumerUid, String portId) {
        List<String> out = new ArrayList<>();
        for (NodeInstance node : ExecChain.reverse(f.graph, consumerUid, portId, false, diagnostics)) {
            out.addAll(emitStatement(f, node));
        }
        return out;
    }

    private List<String> emitStatement(Frame f, NodeInstance node) {
        Optional<NodeType> typeOpt = NodeTypes.get(node.type());
        if (typeOpt.isEmpty()) {
            error("UNKNOWN_NODE_TYPE", "unknown node type '" + node.type() + "'", node.uid());
            return List.of("0");
        }
        NodeType type = typeOpt.get();
        switch (type.kind()) {
            case EXEC_SET_VAR -> {
                // v9 左读右写：target 是右侧输出，连线 variable 节点的 in；左值 = 该 variable 节点的隐式读发射
                Out value = emitValueInput(f, node.uid(), "value");
                NodeInstance targetVar = null;
                for (Wire w : f.graph.wires()) {
                    if (w.from().node().equals(node.uid()) && w.from().port().equals("target")) {
                        targetVar = f.graph.findNode(w.to().node()).orElse(null);
                        break;
                    }
                }
                if (targetVar == null || !NodeTypes.VARIABLE.id().equals(targetVar.type())) {
                    error("SET_TARGET_NOT_VARIABLE", "exec.set_var 的 target 必须连线到 variable 节点", node.uid());
                    return EmitSupport.withPreludes(value, "0");
                }
                Out target = emitValueNode(f, targetVar, NodeTypes.get(targetVar.type()));
                List<String> statements = new ArrayList<>(target.preludes());
                statements.addAll(value.preludes());
                statements.add(target.expr() + " = " + value.expr());
                return statements;
            }
            case EXEC_SET_TEMP -> {
                Out value = emitValueInput(f, node.uid(), "value");
                return EmitSupport.withPreludes(value,
                        EmitSupport.withRoot("temp", EmitSupport.string(node, type, "name"))
                                + " = " + value.expr());
            }
            case EXEC_CALL -> {
                Out call = emitCallLike(f, node, type);
                return EmitSupport.withPreludes(call, call.expr());
            }
            case EXEC_LOOP -> {
                Out count = emitValueInput(f, node.uid(), "count");
                String body = bodyString(f, node);
                return EmitSupport.withPreludes(count, "loop(" + count.expr() + ", { " + body + " })");
            }
            case EXEC_FOREACH -> {
                Out array = emitValueInput(f, node.uid(), "array");
                String var = EmitSupport.string(node, type, "var_name");
                if (!var.startsWith("temp.") && !var.startsWith("variable.")) {
                    var = "temp." + var;
                }
                String body = bodyString(f, node);
                return EmitSupport.withPreludes(array,
                        "for_each(" + var + ", " + array.expr() + ", { " + body + " })");
            }
            case EXEC_BREAK -> {
                return List.of("break");
            }
            case EXEC_CONTINUE -> {
                return List.of("continue");
            }
            case EXEC_RETURN -> {
                Out value = emitValueInput(f, node.uid(), "value");
                return EmitSupport.withPreludes(value, "return " + value.expr());
            }
            default -> {
                error("UNSUPPORTED_NODE",
                        "node type '" + type.id() + "' cannot appear in exec context", node.uid());
                return List.of("0");
            }
        }
    }

    /** loop / for_each 的 body 槽：反向链发射；空 body → "0"。 */
    private String bodyString(Frame f, NodeInstance node) {
        List<String> body = emitExecInput(f, node.uid(), "body");
        return body.isEmpty() ? "0" : String.join("; ", body);
    }

    // ---------- 工具 ----------

    /** JsonElement → molang 字面量：数字去 .0；bool → 1/0；string → 单引号转义。 */
    private String literal(JsonElement e, String nodeUid) {
        String s = MolangLiterals.literal(e);
        if (s != null) return s;
        diagnostics.add(Diagnostic.warning("INVALID_CONSTANT",
                "constant is not a primitive molang literal, using 0", nodeUid));
        return "0";
    }

    private void error(String code, String message) {
        diagnostics.add(Diagnostic.error(code, message));
    }

    private void error(String code, String message, String nodeUid) {
        diagnostics.add(Diagnostic.error(code, message, nodeUid));
    }
}
