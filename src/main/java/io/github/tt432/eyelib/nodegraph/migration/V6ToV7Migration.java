package io.github.tt432.eyelib.nodegraph.migration;

import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * v6 → v7：剥除 decl_variables 桶接线与 declvar- 节点。
 */
public final class V6ToV7Migration implements VersionMigration {
    @Override
    public int fromVersion() {
        return 6;
    }

    @Override
    public GraphLibrary apply(GraphLibrary library) {
        return migrateV6ToV7(library);
    }

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
}
