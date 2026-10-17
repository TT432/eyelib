package io.github.tt432.eyelib.nodegraph.codegen;

import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 一帧 = 一张图的发射上下文。根帧 tempPrefix 为空；子图帧为 {@code sg<K>_}。
 * 计数与发射共享帧：计数填充 useCounts/paramRefs/childFrames，发射填充 extracted。
 */
final class Frame {
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

    Frame(GraphLibrary library, GraphData graph, String tempPrefix) {
        this.graph = graph;
        this.resolver = NodeType.SubgraphResolver.of(library, graph.graphInterface());
        this.tempPrefix = tempPrefix;
    }
}
