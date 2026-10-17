package io.github.tt432.eyelib.nodegraph.codegen;

import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortDef;
import io.github.tt432.eyelib.nodegraph.PortDirection;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * 计数阶段：从槽口/EXEC 时机源沿数据流统计值节点出度，并为 subgraph.call 建立子帧
 * （递归/深度检测在此报告）。统计结果供发射阶段决定 temp 提取。
 */
final class SlotUseCounter {
    /** 子图展开深度上限（规格 §2.4-9 防御性限制）。 */
    private static final int MAX_SUBGRAPH_DEPTH = 32;

    private final GraphLibrary library;
    private final List<Diagnostic> diagnostics;
    /** 计数阶段的子图名栈（递归检测 + 深度限制）。 */
    private final Deque<String> subgraphStack = new ArrayDeque<>();
    /** 全局子图调用计数（名称隔离前缀 K 的来源）。 */
    private int subgraphCallCounter;

    SlotUseCounter(GraphLibrary library, List<Diagnostic> diagnostics) {
        this.library = library;
        this.diagnostics = diagnostics;
    }

    void countValueInput(Frame f, String consumerUid, String portId) {
        Optional<Wire> wire = EmitSupport.wireInto(f.graph, consumerUid, portId);
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

    void countNode(Frame f, NodeInstance node, NodeType type) {
        for (PortDef in : type.inputsOf(node, f.resolver)) {
            // multi 输入是声明通道（decl_*），ref 的 read:/write: 是变量声明通道，均不参与值发射计数
            if (in.direction() == PortDirection.IN && in.type().isValue() && !in.multi()
                    && !io.github.tt432.eyelib.nodegraph.NodeTypes.isVarRefPort(in.id())) {
                countValueInput(f, node.uid(), in.id());
            }
        }
        if (type.kind() == NodeType.Kind.SUBGRAPH_CALL) {
            countSubgraphBody(f, node, type);
        }
    }

    void countExecInput(Frame f, String consumerUid, String portId, boolean report) {
        for (NodeInstance node : ExecChain.reverse(f.graph, consumerUid, portId, report, diagnostics)) {
            Optional<NodeType> type = NodeTypes.get(node.type());
            if (type.isEmpty()) continue;
            for (PortDef in : type.get().inputsOf(node, f.resolver)) {
                if (in.direction() == PortDirection.IN && in.type().isValue() && !in.multi()
                        && !io.github.tt432.eyelib.nodegraph.NodeTypes.isVarRefPort(in.id())) {
                    countValueInput(f, node.uid(), in.id());
                }
            }
            if (type.get().kind() == NodeType.Kind.EXEC_LOOP || type.get().kind() == NodeType.Kind.EXEC_FOREACH) {
                countExecInput(f, node.uid(), "body", report);
            }
        }
    }

    void countExecFrom(Frame f, String sourceUid, String sourcePort, boolean report) {
        for (NodeInstance node : ExecChain.forward(f.graph, sourceUid, sourcePort, report, diagnostics)) {
            Optional<NodeType> type = NodeTypes.get(node.type());
            if (type.isEmpty()) continue;
            for (PortDef in : type.get().inputsOf(node, f.resolver)) {
                if (in.direction() == PortDirection.IN && in.type().isValue() && !in.multi()
                        && !io.github.tt432.eyelib.nodegraph.NodeTypes.isVarRefPort(in.id())) {
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
        String name = EmitSupport.string(call, type, "subgraph");
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
        Frame child = new Frame(library, sub.get(), "sg" + subgraphCallCounter++ + "_");
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

    private void error(String code, String message, String nodeUid) {
        diagnostics.add(Diagnostic.error(code, message, nodeUid));
    }
}
