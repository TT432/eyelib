package io.github.tt432.eyelib.nodegraph.migration;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * v13 → v14：op.binary / op.unary 的 op 选项固化为节点类型身份。
 */
public final class V13ToV14Migration implements VersionMigration {
    @Override
    public int fromVersion() {
        return 13;
    }

    @Override
    public GraphLibrary apply(GraphLibrary library) {
        return migrateV13ToV14(library);
    }

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
                ? NodeTypes.binaryOp(MigrationSupport.optionString(node, "op", "+"))
                : NodeTypes.unaryOp(MigrationSupport.optionString(node, "op", "-"));
        if (opType == null) {
            // 未知符号：回落旧默认（binary → op.add，unary → op.negate）
            opType = java.util.Objects.requireNonNull(binary ? NodeTypes.binaryOp("+") : NodeTypes.unaryOp("-"));
        }
        Map<String, JsonElement> options = new LinkedHashMap<>(node.options());
        options.remove("op");
        return new NodeInstance(node.uid(), opType.id(), node.x(), node.y(), options, node.constants());
    }
}
