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
import io.github.tt432.eyelib.molang.compiler.frontend.MolangToken;
import io.github.tt432.eyelib.molang.compiler.frontend.MolangTokenKind;
import io.github.tt432.eyelib.molang.compiler.frontend.MolangTokenizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 单次槽生成的会话（每个 emit 调用一个实例，保证 §2.4-2 确定性）。
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
    /** 子图展开深度上限（规格 §2.4-9 防御性限制）。 */
    private static final int MAX_SUBGRAPH_DEPTH = 32;
    /** .emolang 自定义函数展开深度上限（规格 nodegraph-emolang-functions §4-5）。 */
    private static final int MAX_EMOLANG_DEPTH = 32;

    /** 可直接文本复制的表达式：全名标识符 / 数字字面量 / 字符串字面量。 */
    private static final Pattern DUPLICATABLE = Pattern.compile(
            "(?:variable|temp|query|math|context|geometry|texture|material)\\.[A-Za-z0-9_.]+"
                    + "|-?[0-9]+(?:\\.[0-9]+)?"
                    + "|'(?:[^'\\\\]|\\\\.)*'");

    private final GraphLibrary library;
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    /** 计数阶段的子图名栈（递归检测 + 深度限制）。 */
    private final Deque<String> subgraphStack = new ArrayDeque<>();
    /** 全局子图调用计数（名称隔离前缀 K 的来源）。 */
    private int subgraphCallCounter;
    /** .emolang 展开的函数名栈（递归检测 + 深度限制）。 */
    private final Deque<String> emolangStack = new ArrayDeque<>();
    /** 全局 .emolang 展开计数（temp.em<K>_ 前缀来源）。 */
    private int emolangCallCounter;

    EmitSession(GraphLibrary library) {
        this.library = library;
    }

    // ---------- 入口 ----------

    CodegenResult emitExpression(String graphName, PortRef slot) {
        Optional<Frame> frame = openSlot(graphName, slot, false);
        if (frame.isEmpty()) {
            return new CodegenResult("0", diagnostics);
        }
        Frame f = frame.get();
        countValueInput(f, slot.node(), slot.port());
        Out out = emitValueInput(f, slot.node(), slot.port());
        List<String> parts = new ArrayList<>(out.preludes());
        parts.add(out.expr());
        return new CodegenResult(String.join("; ", parts), diagnostics);
    }

    CodegenResult emitStatementList(String graphName, PortRef slot) {
        Optional<Frame> frame = openSlot(graphName, slot, true);
        if (frame.isEmpty()) {
            return new CodegenResult("0", diagnostics);
        }
        Frame f = frame.get();
        countExecInput(f, slot.node(), slot.port(), true);
        List<String> statements = emitExecInput(f, slot.node(), slot.port());
        String code = statements.isEmpty() ? "0" : String.join("; ", statements);
        return new CodegenResult(code, diagnostics);
    }

    /** 校验图/节点/端口并建立根帧；失败时报告诊断并返回空。 */
    private Optional<Frame> openSlot(String graphName, PortRef slot, boolean exec) {
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
        Frame frame = new Frame(graph.get(), "");
        Optional<PortDef> port = type.get().inputsOf(node.get(), frame.resolver).stream()
                .filter(p -> p.id().equals(slot.port()) && p.direction() == PortDirection.IN)
                .findFirst();
        boolean ok = exec
                ? port.isPresent() && port.get().type() == PortType.EXEC
                : port.isPresent() && port.get().type().isValue();
        if (!ok) {
            error("INVALID_SLOT",
                    "port '" + slot.port() + "' of node '" + slot.node() + "' is not an "
                            + (exec ? "exec" : "value") + " input slot", slot.node());
            return Optional.empty();
        }
        return Optional.of(frame);
    }

    // ---------- 计数（出度统计 + 子图预展开） ----------

    private void countValueInput(Frame f, String consumerUid, String portId) {
        Optional<Wire> wire = wireInto(f.graph, consumerUid, portId);
        if (wire.isEmpty()) return;
        Optional<NodeInstance> producer = f.graph.findNode(wire.get().from().node());
        if (producer.isEmpty()) return;
        NodeInstance pn = producer.get();
        Optional<NodeType> pt = NodeTypes.get(pn.type());
        if (pt.isPresent() && pt.get().kind() == NodeType.Kind.SUBGRAPH_INPUT) {
            // 子图内对形参锚点输出端口的引用 → 记入形参引用计数
            f.paramRefs.merge(wire.get().from().port(), 1, Integer::sum);
            return;
        }
        f.useCounts.merge(pn.uid(), 1, Integer::sum);
        if (f.counted.add(pn.uid()) && pt.isPresent()) {
            countNode(f, pn, pt.get());
        }
    }

    private void countNode(Frame f, NodeInstance node, NodeType type) {
        for (PortDef in : type.inputsOf(node, f.resolver)) {
            // multi 输入是声明通道（decl_*），不参与值发射计数
            if (in.direction() == PortDirection.IN && in.type().isValue() && !in.multi()) {
                countValueInput(f, node.uid(), in.id());
            }
        }
        if (type.kind() == NodeType.Kind.SUBGRAPH_CALL) {
            countSubgraphBody(f, node, type);
        }
    }

    private void countExecInput(Frame f, String consumerUid, String portId, boolean report) {
        for (NodeInstance node : resolveChain(f, consumerUid, portId, report)) {
            Optional<NodeType> type = NodeTypes.get(node.type());
            if (type.isEmpty()) continue;
            for (PortDef in : type.get().inputsOf(node, f.resolver)) {
                if (in.direction() == PortDirection.IN && in.type().isValue() && !in.multi()) {
                    countValueInput(f, node.uid(), in.id());
                }
            }
            if (type.get().kind() == NodeType.Kind.EXEC_LOOP || type.get().kind() == NodeType.Kind.EXEC_FOREACH) {
                countExecInput(f, node.uid(), "body", report);
            }
        }
    }

    /** 为 subgraph.call 建立子帧并计数子图内部 DAG（递归/深度检测在此报告）。 */
    private void countSubgraphBody(Frame parent, NodeInstance call, NodeType type) {
        String name = string(call, type, "subgraph");
        Optional<GraphData> sub = name.isEmpty() ? Optional.empty() : library.graph(name);
        if (sub.isEmpty() || sub.get().graphInterface().isEmpty()) {
            error("UNKNOWN_SUBGRAPH", "subgraph '" + name + "' not found in library", call.uid());
            return;
        }
        if (subgraphStack.contains(name) || subgraphStack.size() >= MAX_SUBGRAPH_DEPTH) {
            error("SUBGRAPH_RECURSION",
                    "subgraph '" + name + "' expands recursively or exceeds max depth " + MAX_SUBGRAPH_DEPTH,
                    call.uid());
            return;
        }
        Frame child = new Frame(sub.get(), "sg" + subgraphCallCounter++ + "_");
        parent.childFrames.put(call.uid(), child);
        subgraphStack.push(name);
        try {
            Optional<NodeInstance> anchor = sub.get().nodes().stream()
                    .filter(n -> NodeTypes.get(n.type())
                            .map(t -> t.kind() == NodeType.Kind.SUBGRAPH_OUTPUT).orElse(false))
                    .min(Comparator.comparing(NodeInstance::uid));
            if (anchor.isEmpty()) {
                error("SUBGRAPH_NO_OUTPUT", "subgraph '" + name + "' has no subgraph.output anchor", call.uid());
                return;
            }
            child.outputAnchor = anchor;
            countValueInput(child, anchor.get().uid(), "result");
            countExecInput(child, anchor.get().uid(), "exec_in", true);
        } finally {
            subgraphStack.pop();
        }
    }

    // ---------- 值发射 ----------

    /** 发射某节点的值输入端口：已连线 → 发射生产者；否则 constants → 端口默认 → UNCONNECTED_INPUT。 */
    private Out emitValueInput(Frame f, String consumerUid, String portId) {
        Optional<Wire> wire = wireInto(f.graph, consumerUid, portId);
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
        Frame f = new Frame(graph.get(), "");
        boolean valid = type.get().outputsOf(node.get(), f.resolver).stream()
                .anyMatch(p -> p.id().equals(portId) && p.type().isValue());
        if (!valid) {
            error("INVALID_SLOT", "port '" + portId + "' of node '" + nodeUid + "' is not a value output port", nodeUid);
            return new CodegenResult("0", diagnostics);
        }
        countNode(f, node.get(), type.get());
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
        Optional<Wire> wire = wireInto(graph.get(), slot.node(), slot.port());
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
                float[] c = ColorValues.parse(string(producer.get(), type.get(), "value"));
                if (c == null) {
                    error("INVALID_COLOR_VALUE", "const.color 的 value 不是 #RRGGBB/#AARRGGBB，按白色处理",
                            producer.get().uid());
                    c = new float[]{1, 1, 1, 1};
                }
                yield new ColorCodegenResult(new ColorCode(
                        formatNumber(c[0]), formatNumber(c[1]), formatNumber(c[2]), formatNumber(c[3])),
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
        Frame f = new Frame(graph, "");
        countValueInput(f, compose.uid(), channel);
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
        if (uses >= 2 && type.isPresent() && !isTrivial(type.get(), node)) {
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
            case CONST_NUMBER, CONST_INT -> Out.of(number(node, type, "value"));
            case CONST_BOOL -> Out.of(bool(node, type, "value") ? "1" : "0");
            case CONST_STRING -> Out.of(quote(string(node, type, "value")));
            case CONST_COLOR, COLOR_COMPOSE -> {
                // 颜色是复合值，只能经 emitColor 进入颜色端口；流到标量上下文即图有误
                error("COLOR_AS_SCALAR", "color value cannot be used as a scalar molang expression", node.uid());
                yield Out.of("0");
            }
            // 变量根按黑板声明的 molang 作用域选择（规格 nodegraph-variable-table §2.2）；
            // 未声明回落 variable.*（验证器另有 UNDECLARED_VARIABLE 警告）
            case VARIABLE -> {
                String varName = string(node, type, "name");
                String bare = varName.startsWith("variable.") || varName.startsWith("temp.")
                        ? varName.substring(varName.indexOf('.') + 1) : varName;
                boolean temp = GraphVariableOps.find(f.graph, bare)
                        .map(d -> d.scope() == VariableDecl.Scope.TEMP)
                        .orElse(false);
                yield Out.of(withRoot(temp ? "temp" : "variable", varName));
            }
            case TEMP_GET -> Out.of(withRoot("temp", string(node, type, "name")));
            case CONTEXT_GET -> Out.of(withRoot("context", string(node, type, "name")));
            case QUERY_CALL, MATH_CALL -> emitCallLike(f, node, type);
            case OP_BINARY -> {
                Out a = emitValueInput(f, node.uid(), "a");
                Out b = emitValueInput(f, node.uid(), "b");
                yield combine(List.of(a, b),
                        "(" + a.expr() + " " + string(node, type, "op") + " " + b.expr() + ")");
            }
            case OP_UNARY -> {
                Out a = emitValueInput(f, node.uid(), "a");
                yield combine(List.of(a), "(" + string(node, type, "op") + a.expr() + ")");
            }
            case OP_TERNARY -> {
                Out cond = emitValueInput(f, node.uid(), "cond");
                Out a = emitValueInput(f, node.uid(), "a");
                Out b = emitValueInput(f, node.uid(), "b");
                yield combine(List.of(cond, a, b),
                        "(" + cond.expr() + " ? " + a.expr() + " : " + b.expr() + ")");
            }
            case OP_NULLCOALESCE -> {
                Out a = emitValueInput(f, node.uid(), "a");
                Out b = emitValueInput(f, node.uid(), "b");
                yield combine(List.of(a, b), "(" + a.expr() + " ?? " + b.expr() + ")");
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
        String function = string(node, type, "function");
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
            boolean wired = wireInto(f.graph, node.uid(), in.id()).isPresent();
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
        Out expanded = expandEmolang(fn, args, node.uid());
        preludes.addAll(expanded.preludes());
        return new Out(preludes, expanded.expr());
    }

    /**
     * 递归展开函数体：token 级形参替换（引用 ≥2 且非平凡 → temp.em<K>_ 提取前置）、
     * 体内 temp.* 一律卫生重命名为 temp.em<K>_<原名>、嵌套自定义调用同机制递归。
     * 前置语句与体语句按语句边界交错落位（体前段语句的 variable 副作用对后段嵌套调用的
     * 实参可见，顺序与手写 molang 一致）。
     */
    private Out expandEmolang(EmolangFunction fn, List<String> argExprs, String nodeUid) {
        if (emolangStack.contains(fn.name()) || emolangStack.size() >= MAX_EMOLANG_DEPTH) {
            error("EMOLANG_RECURSION",
                    "emolang '" + fn.name() + "' expands recursively or exceeds max depth "
                            + MAX_EMOLANG_DEPTH, nodeUid);
            return Out.of("0");
        }
        emolangStack.push(fn.name());
        try {
            String prefix = "em" + (emolangCallCounter++) + "_";
            List<EmolangFunction.Param> params = fn.params();
            List<MolangToken> body = fn.body();

            // 形参引用计数（token 级；成员访问段不算引用）
            Map<String, Integer> refCounts = new HashMap<>();
            for (int i = 0; i < body.size(); i++) {
                MolangToken t = body.get(i);
                if (t.kind() == MolangTokenKind.IDENTIFIER
                        && (i == 0 || body.get(i - 1).kind() != MolangTokenKind.DOT)) {
                    refCounts.merge(t.lexeme(), 1, Integer::sum);
                }
            }

            // 形参绑定（与子图展开 D5 同规则）
            Map<String, String> bindings = new HashMap<>();
            List<String> preludes = new ArrayList<>();
            for (int i = 0; i < params.size(); i++) {
                EmolangFunction.Param param = params.get(i);
                int refs = refCounts.getOrDefault(param.name(), 0);
                if (refs == 0) {
                    continue;
                }
                String expr = argExprs.get(i);
                if (refs >= 2 && !isDuplicatable(expr)) {
                    String bound = "temp." + prefix + param.name();
                    preludes.add(bound + " = " + expr);
                    bindings.put(param.name(), bound);
                } else {
                    bindings.put(param.name(), "(" + expr + ")");
                }
            }

            // 体语句逐句替换、嵌套前置随句落位；return 段最后处理
            for (List<MolangToken> stmt : splitStatements(body.subList(0, fn.returnIndex()))) {
                List<MolangToken> substituted = substitute(stmt, bindings, prefix, nodeUid, preludes);
                String text = joinTokens(substituted);
                if (!text.isEmpty()) {
                    preludes.add(text);
                }
            }
            List<MolangToken> returnTokens = new ArrayList<>(body.subList(fn.returnIndex() + 1, body.size()));
            if (!returnTokens.isEmpty()
                    && returnTokens.get(returnTokens.size() - 1).kind() == MolangTokenKind.SEMICOLON) {
                returnTokens.remove(returnTokens.size() - 1);
            }
            List<MolangToken> substitutedReturn = substitute(returnTokens, bindings, prefix, nodeUid, preludes);
            return new Out(preludes, joinTokens(substitutedReturn));
        } finally {
            emolangStack.pop();
        }
    }

    /**
     * token 流替换：形参 → 绑定文本、temp/t 别名成员 → 卫生重命名、裸名自定义调用 →
     * 递归展开（其实参段先经本替换——外层形参/局部变量在嵌套实参中保持可见）。
     * 嵌套展开产生的前置语句汇入 {@code preludeSink}（调用方按语句边界落位）。
     */
    private List<MolangToken> substitute(List<MolangToken> tokens, Map<String, String> bindings,
                                         String prefix, String nodeUid, List<String> preludeSink) {
        List<MolangToken> out = new ArrayList<>(tokens.size());
        for (int i = 0; i < tokens.size(); i++) {
            MolangToken t = tokens.get(i);
            if (t.kind() == MolangTokenKind.IDENTIFIER
                    && (i == 0 || tokens.get(i - 1).kind() != MolangTokenKind.DOT)) {
                String bound = bindings.get(t.lexeme());
                if (bound != null) {
                    out.addAll(MolangTokenizer.tokenize(bound));
                    continue;
                }
                if (("temp".equals(t.lexeme()) || "t".equals(t.lexeme()))
                        && i + 2 < tokens.size()
                        && tokens.get(i + 1).kind() == MolangTokenKind.DOT
                        && tokens.get(i + 2).kind() == MolangTokenKind.IDENTIFIER) {
                    out.addAll(MolangTokenizer.tokenize("temp." + prefix + tokens.get(i + 2).lexeme()));
                    i += 2;
                    continue;
                }
                EmolangFunction nested = EmolangRegistry.find(t.lexeme());
                if (nested != null && i + 1 < tokens.size()
                        && tokens.get(i + 1).kind() == MolangTokenKind.LEFT_PAREN) {
                    int close = matchingParen(tokens, i + 1);
                    if (close < 0) {
                        error("EMOLANG_SYNTAX", "unclosed call in emolang body", nodeUid);
                        continue;
                    }
                    // 嵌套实参段先做外层替换（外层形参/局部变量可见），再切分展开
                    List<MolangToken> nestedArgTokens = substitute(
                            tokens.subList(i + 2, close), bindings, prefix, nodeUid, preludeSink);
                    Out nestedOut = expandEmolang(nested, splitArgs(nestedArgTokens), nodeUid);
                    preludeSink.addAll(nestedOut.preludes());
                    out.addAll(MolangTokenizer.tokenize("(" + nestedOut.expr() + ")"));
                    i = close;
                    continue;
                }
            }
            out.add(t);
        }
        // 复 tokenizer 会附 EOF，剥掉
        out.removeIf(tok -> tok.kind() == MolangTokenKind.EOF);
        return out;
    }

    /** 体语句切分：顶层分号 / 回到深度 0 的右大括号（loop/for_each 块）为语句尾。 */
    private static List<List<MolangToken>> splitStatements(List<MolangToken> tokens) {
        List<List<MolangToken>> statements = new ArrayList<>();
        List<MolangToken> current = new ArrayList<>();
        int depth = 0;
        for (MolangToken t : tokens) {
            current.add(t);
            if (t.kind() == MolangTokenKind.LEFT_BRACE) depth++;
            else if (t.kind() == MolangTokenKind.RIGHT_BRACE) depth--;
            if (depth == 0 && (t.kind() == MolangTokenKind.SEMICOLON
                    || t.kind() == MolangTokenKind.RIGHT_BRACE)) {
                statements.add(current);
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            statements.add(current);
        }
        return statements;
    }

    /** token 流重建文本：分号/逗号/括号/点号贴合，其余空格分隔；尾部分号剥除。 */
    private static String joinTokens(List<MolangToken> tokens) {
        StringBuilder sb = new StringBuilder();
        String prev = null;
        for (MolangToken t : tokens) {
            String lexeme = t.lexeme();
            boolean tightBefore = "." .equals(lexeme) || ";".equals(lexeme) || ",".equals(lexeme)
                    || ")".equals(lexeme) || "]".equals(lexeme);
            boolean prevTightAfter = ".".equals(prev) || "(".equals(prev) || "[".equals(prev);
            if (prev != null && !tightBefore && !prevTightAfter) {
                sb.append(' ');
            }
            sb.append(lexeme);
            prev = lexeme;
        }
        String s = sb.toString().strip();
        while (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1).strip();
        }
        return s;
    }

    /** 配对的右括号下标（openIdx 处须为 LEFT_PAREN）；未闭合 → -1。 */
    private static int matchingParen(List<MolangToken> tokens, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < tokens.size(); i++) {
            MolangTokenKind k = tokens.get(i).kind();
            if (k == MolangTokenKind.LEFT_PAREN) depth++;
            else if (k == MolangTokenKind.RIGHT_PAREN) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    /** 实参 token 段按顶层逗号切分并各自重建文本（空段列表 → 空表）。 */
    private static List<String> splitArgs(List<MolangToken> tokens) {
        List<String> args = new ArrayList<>();
        int depth = 0;
        List<MolangToken> current = new ArrayList<>();
        for (MolangToken t : tokens) {
            MolangTokenKind k = t.kind();
            if (k == MolangTokenKind.COMMA && depth == 0) {
                args.add(joinTokens(current));
                current = new ArrayList<>();
                continue;
            }
            if (k == MolangTokenKind.LEFT_PAREN || k == MolangTokenKind.LEFT_BRACKET
                    || k == MolangTokenKind.LEFT_BRACE) depth++;
            if (k == MolangTokenKind.RIGHT_PAREN || k == MolangTokenKind.RIGHT_BRACKET
                    || k == MolangTokenKind.RIGHT_BRACE) depth--;
            current.add(t);
        }
        if (!current.isEmpty()) {
            args.add(joinTokens(current));
        }
        return args;
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
            if (refs >= 2 && !isDuplicatable(arg.expr())) {
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
        for (NodeInstance node : resolveChain(f, consumerUid, portId, false)) {
            out.addAll(emitStatement(f, node));
        }
        return out;
    }

    /**
     * 解析 exec 链：返回链首→链尾的节点序列。
     * 槽口/前驱 exec_out 的连线指向链尾；节点缺 exec_in 连接即链首。空槽 → 空序列。
     */
    private List<NodeInstance> resolveChain(Frame f, String consumerUid, String portId, boolean report) {
        Optional<Wire> wire = wireInto(f.graph, consumerUid, portId);
        if (wire.isEmpty()) return List.of();
        List<NodeInstance> reversed = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String current = wire.get().from().node();
        while (true) {
            if (!visited.add(current)) {
                if (report) error("EXEC_CYCLE", "exec chain contains a cycle at node '" + current + "'", current);
                break;
            }
            Optional<NodeInstance> node = f.graph.findNode(current);
            if (node.isEmpty()) {
                if (report) error("UNKNOWN_NODE", "exec chain node '" + current + "' not found", current);
                break;
            }
            reversed.add(0, node.get());
            Optional<Wire> back = wireInto(f.graph, node.get().uid(), "exec_in");
            if (back.isEmpty()) break;
            current = back.get().from().node();
        }
        return reversed;
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
                // target 引脚接 variable 节点；其隐式读发射即 variable.<name>，直接用作赋值左值
                Out target = emitValueInput(f, node.uid(), "target");
                Out value = emitValueInput(f, node.uid(), "value");
                List<String> statements = new ArrayList<>(target.preludes());
                statements.addAll(value.preludes());
                statements.add(target.expr() + " = " + value.expr());
                return statements;
            }
            case EXEC_SET_TEMP -> {
                Out value = emitValueInput(f, node.uid(), "value");
                return withPreludes(value, withRoot("temp", string(node, type, "name")) + " = " + value.expr());
            }
            case EXEC_CALL -> {
                Out call = emitCallLike(f, node, type);
                return withPreludes(call, call.expr());
            }
            case EXEC_LOOP -> {
                Out count = emitValueInput(f, node.uid(), "count");
                String body = bodyString(f, node);
                return withPreludes(count, "loop(" + count.expr() + ", { " + body + " })");
            }
            case EXEC_FOREACH -> {
                Out array = emitValueInput(f, node.uid(), "array");
                String var = string(node, type, "var_name");
                if (!var.startsWith("temp.") && !var.startsWith("variable.")) {
                    var = "temp." + var;
                }
                String body = bodyString(f, node);
                return withPreludes(array, "for_each(" + var + ", " + array.expr() + ", { " + body + " })");
            }
            case EXEC_BREAK -> {
                return List.of("break");
            }
            case EXEC_CONTINUE -> {
                return List.of("continue");
            }
            case EXEC_RETURN -> {
                Out value = emitValueInput(f, node.uid(), "value");
                return withPreludes(value, "return " + value.expr());
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

    /** 永不提取的平凡节点：常量、单变量/属性引用（§2.4-3）。 */
    private static boolean isTrivial(NodeType type, NodeInstance node) {
        return switch (type.kind()) {
            case CONST_NUMBER, CONST_INT, CONST_BOOL, CONST_STRING, CONST_COLOR, COLOR_COMPOSE,
                 VARIABLE, TEMP_GET, CONTEXT_GET,
                 REF_GEOMETRY, REF_TEXTURE, REF_MATERIAL -> true;
            case QUERY_CALL, MATH_CALL -> node.optionInt("arg_count", 0) <= 0;
            default -> false;
        };
    }

    private static boolean isDuplicatable(String expr) {
        return DUPLICATABLE.matcher(expr).matches();
    }

    private static String withRoot(String root, String name) {
        return name.startsWith(root + ".") ? name : root + "." + name;
    }

    private static String string(NodeInstance node, NodeType type, String id) {
        return node.option(id, type).map(JsonElement::getAsString).orElse("");
    }

    private static String number(NodeInstance node, NodeType type, String id) {
        return node.option(id, type)
                .filter(JsonElement::isJsonPrimitive)
                .map(e -> formatNumber(e.getAsDouble()))
                .orElse("0");
    }

    private static boolean bool(NodeInstance node, NodeType type, String id) {
        return node.option(id, type)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsBoolean)
                .orElse(false);
    }

    /** JsonElement → molang 字面量：数字去 .0；bool → 1/0；string → 单引号转义。 */
    private String literal(JsonElement e, String nodeUid) {
        String s = MolangLiterals.literal(e);
        if (s != null) return s;
        diagnostics.add(Diagnostic.warning("INVALID_CONSTANT",
                "constant is not a primitive molang literal, using 0", nodeUid));
        return "0";
    }

    /** 数字格式化：整数去 .0（1.0 → "1"，1.5 → "1.5"）。 */
    static String formatNumber(double v) {
        return MolangLiterals.formatNumber(v);
    }

    /** 字符串字面量：单引号包裹，转义 ' 与 \。 */
    static String quote(String s) {
        return MolangLiterals.quote(s);
    }

    /** 多条入线（非法单连接）时取 from (node, port) 字典序最小者，保证确定性。 */
    private static Optional<Wire> wireInto(GraphData graph, String nodeUid, String portId) {
        return graph.wires().stream()
                .filter(w -> w.to().node().equals(nodeUid) && w.to().port().equals(portId))
                .min(Comparator.comparing((Wire w) -> w.from().node()).thenComparing(w -> w.from().port()));
    }

    private static Out combine(List<Out> parts, String expr) {
        List<String> preludes = new ArrayList<>();
        for (Out part : parts) {
            preludes.addAll(part.preludes());
        }
        return new Out(preludes, expr);
    }

    private static List<String> withPreludes(Out out, String statement) {
        List<String> statements = new ArrayList<>(out.preludes());
        statements.add(statement);
        return statements;
    }

    private void error(String code, String message) {
        diagnostics.add(Diagnostic.error(code, message));
    }

    private void error(String code, String message, String nodeUid) {
        diagnostics.add(Diagnostic.error(code, message, nodeUid));
    }

    /** 值发射结果：前置语句（temp 提取/子图语句）+ 表达式文本。 */
    private record Out(List<String> preludes, String expr) {
        static Out of(String expr) {
            return new Out(List.of(), expr);
        }
    }

    /**
     * 一帧 = 一张图的发射上下文。根帧 tempPrefix 为空；子图帧为 {@code sg<K>_}。
     * 计数与发射共享帧：计数填充 useCounts/paramRefs/childFrames，发射填充 extracted。
     */
    private final class Frame {
        final GraphData graph;
        final NodeType.SubgraphResolver resolver;
        final String tempPrefix;
        /** 值节点 uid → 本槽 DAG 内使用次数（出度）。 */
        final Map<String, Integer> useCounts = new HashMap<>();
        /** 计数阶段已遍历输入的值节点（防重）。 */
        final Set<String> counted = new HashSet<>();
        /** 子图帧：形参名 → 子图内引用次数。 */
        final Map<String, Integer> paramRefs = new HashMap<>();
        /** 已提取节点 uid → temp 名。 */
        final Map<String, String> extracted = new HashMap<>();
        /** subgraph.call 节点 uid → 子帧。 */
        final Map<String, Frame> childFrames = new HashMap<>();
        /** 子图帧：形参名 → 绑定表达式文本（发射阶段填充）。 */
        Map<String, String> bindings = Map.of();
        /** 子图帧：subgraph.output 锚点（计数阶段定位）。 */
        Optional<NodeInstance> outputAnchor = Optional.empty();
        int tempCounter;

        Frame(GraphData graph, String tempPrefix) {
            this.graph = graph;
            this.resolver = NodeType.SubgraphResolver.of(library, graph.graphInterface());
            this.tempPrefix = tempPrefix;
        }
    }
}
