package io.github.tt432.eyelib.nodegraph.codegen;

import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** EXEC 链的纯遍历：正向（时机源端口 → 链尾）与反向（槽口 → 链首）。 */
final class ExecChain {
    private ExecChain() {
    }

    /**
     * 正向解析 exec 链（v13 时机源模型）：源端口出线指向链首，沿 exec_out 逐节点前进。
     * 返回链首→链尾序列；源端口未连线 → 空序列。
     */
    static List<NodeInstance> forward(GraphData graph, String sourceUid, String sourcePort,
                                      boolean report, List<Diagnostic> diagnostics) {
        Optional<Wire> first = EmitSupport.wireOutOf(graph, sourceUid, sourcePort);
        if (first.isEmpty()) return List.of();
        List<NodeInstance> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String current = first.get().to().node();
        while (true) {
            if (!visited.add(current)) {
                if (report) error(diagnostics, "EXEC_CYCLE",
                        "exec chain contains a cycle at node '" + current + "'", current);
                break;
            }
            Optional<NodeInstance> node = graph.findNode(current);
            if (node.isEmpty()) {
                if (report) error(diagnostics, "UNKNOWN_NODE",
                        "exec chain node '" + current + "' not found", current);
                break;
            }
            chain.add(node.get());
            Optional<Wire> next = EmitSupport.wireOutOf(graph, current, "exec_out");
            if (next.isEmpty()) break;
            current = next.get().to().node();
        }
        return chain;
    }

    /**
     * 解析 exec 链：返回链首→链尾的节点序列。
     * 槽口/前驱 exec_out 的连线指向链尾；节点缺 exec_in 连接即链首。空槽 → 空序列。
     */
    static List<NodeInstance> reverse(GraphData graph, String consumerUid, String portId,
                                      boolean report, List<Diagnostic> diagnostics) {
        Optional<Wire> wire = EmitSupport.wireInto(graph, consumerUid, portId);
        if (wire.isEmpty()) return List.of();
        List<NodeInstance> reversed = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String current = wire.get().from().node();
        while (true) {
            if (!visited.add(current)) {
                if (report) error(diagnostics, "EXEC_CYCLE",
                        "exec chain contains a cycle at node '" + current + "'", current);
                break;
            }
            Optional<NodeInstance> node = graph.findNode(current);
            if (node.isEmpty()) {
                if (report) error(diagnostics, "UNKNOWN_NODE",
                        "exec chain node '" + current + "' not found", current);
                break;
            }
            reversed.add(0, node.get());
            Optional<Wire> back = EmitSupport.wireInto(graph, node.get().uid(), "exec_in");
            if (back.isEmpty()) break;
            current = back.get().from().node();
        }
        return reversed;
    }

    private static void error(List<Diagnostic> diagnostics, String code, String message, String nodeUid) {
        diagnostics.add(Diagnostic.error(code, message, nodeUid));
    }
}
