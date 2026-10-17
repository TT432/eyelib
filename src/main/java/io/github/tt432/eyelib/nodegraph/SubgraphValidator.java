package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 子图相关校验：锚点数量、调用目标存在性、调用链环检测与展开深度。
 *
 * <p>纯静态工具，调用图以 {@link CallEdge} 列表表达；不持有任何可变状态。
 */
final class SubgraphValidator {

    private SubgraphValidator() {
    }

    /** 检查 15：子图锚点数量；主图不得有锚点。 */
    static void checkSubgraphAnchors(GraphLibrary library, List<Diagnostic> out) {
        for (Map.Entry<String, GraphData> e : library.graphs().entrySet()) {
            String name = e.getKey();
            GraphData graph = e.getValue();
            long inputs = graph.nodes().stream().filter(n -> n.type().equals("subgraph.input")).count();
            long outputs = graph.nodes().stream().filter(n -> n.type().equals("subgraph.output")).count();
            boolean isMain = name.equals(library.main());
            if (isMain) {
                if (inputs > 0 || outputs > 0) {
                    out.add(Diagnostic.error(GraphValidator.SUBGRAPH_ANCHOR,
                            "主图不得包含 subgraph.input/subgraph.output 锚点（input=" + inputs + ", output=" + outputs + "）"));
                }
            } else if (graph.isSubgraph()) {
                if (outputs != 1) {
                    out.add(Diagnostic.error(GraphValidator.SUBGRAPH_ANCHOR,
                            "子图 '" + name + "' 必须恰好 1 个 subgraph.output，实际 " + outputs));
                }
                if (inputs > 1) {
                    out.add(Diagnostic.error(GraphValidator.SUBGRAPH_ANCHOR,
                            "子图 '" + name + "' 至多 1 个 subgraph.input，实际 " + inputs));
                }
            }
        }
    }

    /** 子图调用边（目标图名 + 调用节点 uid）。 */
    record CallEdge(String target, String nodeUid) {
    }

    /** 检查 13：subgraph.call 指向存在且有接口的子图；同时返回调用图（供递归/引用冲突检查）。 */
    static Map<String, List<CallEdge>> checkSubgraphTargets(GraphLibrary library, List<Diagnostic> out) {
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
                    out.add(Diagnostic.error(GraphValidator.SUBGRAPH_TARGET,
                            "subgraph.call 指向的子图 '" + target + "' 不存在或不是子图（无 interface）", node.uid()));
                } else {
                    edges.add(new CallEdge(target, node.uid()));
                }
            }
            callGraph.put(e.getKey(), edges);
        }
        return callGraph;
    }

    /** 检查 14：子图调用链无环且展开深度 ≤ {@link GraphValidator#MAX_SUBGRAPH_DEPTH}。 */
    static void checkSubgraphRecursion(GraphLibrary library, Map<String, List<CallEdge>> callGraph,
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
        if (depth > GraphValidator.MAX_SUBGRAPH_DEPTH) {
            out.add(Diagnostic.error(GraphValidator.SUBGRAPH_RECURSION,
                    "子图展开深度 " + depth + " 超过上限 " + GraphValidator.MAX_SUBGRAPH_DEPTH));
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
                    out.add(Diagnostic.error(GraphValidator.SUBGRAPH_RECURSION,
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
}
