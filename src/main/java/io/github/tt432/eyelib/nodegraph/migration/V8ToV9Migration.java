package io.github.tt432.eyelib.nodegraph.migration;

import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * v8 → v9：变量写入通道方向翻转（左读右写）。
 */
public final class V8ToV9Migration implements VersionMigration {
    @Override
    public int fromVersion() {
        return 8;
    }

    @Override
    public GraphLibrary apply(GraphLibrary library) {
        return migrateV8ToV9(library);
    }

    /**
     * v9 变量流向重排（用户决策 2026-08-09：左读右写，位置即语义）：
     * <ul>
     *   <li>exec.set_var.target 从左侧输入改为右侧输出：v8 连线
     *       {@code variable.out → set_var.target} 翻转为 {@code set_var.target → variable.in}；</li>
     *   <li>ref.animation 的 write:<名> 端口从输入改为输出：v8 连线
     *       {@code declvar.out → ref.write:<名>} 翻转为 {@code ref.write:<名> → declvar.in}；</li>
     *   <li>variable 节点新增 multi 输入 in（写入通道）。
     *       read: 端口方向不变。写入边自此不参与环检测与布局分层。</li>
     * </ul>
     */
    private static GraphLibrary migrateV8ToV9(GraphLibrary library) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            GraphData g = entry.getValue();
            Set<String> setVarUids = new HashSet<>();
            Set<String> animRefUids = new HashSet<>();
            for (NodeInstance n : g.nodes()) {
                if (NodeTypes.EXEC_SET_VAR.id().equals(n.type())) {
                    setVarUids.add(n.uid());
                } else if (NodeTypes.REF_ANIMATION.id().equals(n.type())) {
                    animRefUids.add(n.uid());
                }
            }
            List<Wire> wires = new ArrayList<>();
            for (Wire w : g.wires()) {
                if (setVarUids.contains(w.to().node()) && "target".equals(w.to().port())) {
                    wires.add(new Wire(new PortRef(w.to().node(), "target"),
                            new PortRef(w.from().node(), "in")));
                } else if (animRefUids.contains(w.to().node())
                        && w.to().port().startsWith(NodeTypes.VAR_WRITE_PREFIX)) {
                    wires.add(new Wire(new PortRef(w.to().node(), w.to().port()),
                            new PortRef(w.from().node(), "in")));
                } else {
                    wires.add(w);
                }
            }
            graphs.put(entry.getKey(), new GraphData(g.nodes(), wires, g.variables(),
                    g.placemats(), g.stickyNotes(), g.graphInterface()));
        }
        return new GraphLibrary(library.formatVersion(), library.kind(), library.main(), graphs);
    }
}
