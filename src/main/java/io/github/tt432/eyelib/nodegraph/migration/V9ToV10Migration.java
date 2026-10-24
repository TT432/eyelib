package io.github.tt432.eyelib.nodegraph.migration;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * v9 → v10：AC initial_state / transition.target 字符串选项图边化。
 */
public final class V9ToV10Migration implements VersionMigration {
    @Override
    public int fromVersion() {
        return 9;
    }

    @Override
    public GraphLibrary apply(GraphLibrary library) {
        return migrateV9ToV10(library);
    }

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
}
