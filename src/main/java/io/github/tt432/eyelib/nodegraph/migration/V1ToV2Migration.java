package io.github.tt432.eyelib.nodegraph.migration;

import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * v1 → v2：变量节点化（var.get / exec.set_var 改造）。
 */
public final class V1ToV2Migration implements VersionMigration {
    @Override
    public int fromVersion() {
        return 1;
    }

    @Override
    public GraphLibrary apply(GraphLibrary library) {
        return migrateV1ToV2(library);
    }

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
                    String name = MigrationSupport.optionString(node, "name", "");
                    nodes.add(new NodeInstance(node.uid(), "variable", node.x(), node.y(),
                            Map.of("name", new JsonPrimitive(MigrationSupport.stripRoot(name, "variable"))),
                            node.constants()));
                }
                case "exec.set_var" -> {
                    if (!node.options().containsKey("name")) {
                        // 新格式（target 引脚、无选项）原样保留
                        nodes.add(node);
                        break;
                    }
                    String root = MigrationSupport.optionString(node, "root", "variable");
                    String name = MigrationSupport.optionString(node, "name", "");
                    if ("temp".equals(root)) {
                        nodes.add(new NodeInstance(node.uid(), "exec.set_temp", node.x(), node.y(),
                                Map.of("name", new JsonPrimitive(name)), node.constants()));
                    } else {
                        // 新 exec.set_var（无选项）+ 自动 variable 节点连线 target
                        String varUid = MigrationSupport.freshUid(uidTaken, counter);
                        nodes.add(new NodeInstance(node.uid(), "exec.set_var", node.x(), node.y(),
                                Map.of(), node.constants()));
                        nodes.add(new NodeInstance(varUid, "variable", node.x() - 180, node.y() + 30,
                                Map.of("name", new JsonPrimitive(MigrationSupport.stripRoot(name, "variable"))), Map.of()));
                        wires.add(new Wire(new PortRef(varUid, "out"), new PortRef(node.uid(), "target")));
                    }
                }
                default -> nodes.add(node);
            }
        }
        return new GraphData(List.copyOf(nodes), List.copyOf(wires), graph.variables(),
                graph.placemats(), graph.stickyNotes(), graph.graphInterface());
    }
}
