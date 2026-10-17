package io.github.tt432.eyelib.nodegraph.migration;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * v7 → v8：撤除 ref.ac 的命名变量端口与 var_refs 快照。
 */
public final class V7ToV8Migration implements VersionMigration {
    @Override
    public int fromVersion() {
        return 7;
    }

    @Override
    public GraphLibrary apply(GraphLibrary library) {
        return migrateV7ToV8(library);
    }

    /**
     * v7 给 ref.ac 也加了 read:/write: 命名变量端口与 declvar- 接线；v8 撤除——AC 的变量
     * 引用由 AC 图自身承担（ac.state 的 on_entry/on_exit exec 链内 variable 节点），
     * ref.ac 回到纯引用节点（规格 nodegraph-animation-variable-refs §2.1，用户决策
     * 2026-08-08：AC 类似 RC 图化，非类似 Animation）。剥除 ref.ac 的 var_refs 快照、
     * 指向其 read:/write: 端口的连线、以及因此失去全部出边的 declvar- 节点；
     * ref.animation 的命名端口与接线保留。v7 合入的 AC 源变量声明保留（无副作用，
     * 用户可在变量表删除）。
     */
    private static GraphLibrary migrateV7ToV8(GraphLibrary library) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            GraphData g = entry.getValue();
            Set<String> acRefUids = new HashSet<>();
            for (NodeInstance n : g.nodes()) {
                if (NodeTypes.REF_AC.id().equals(n.type())) {
                    acRefUids.add(n.uid());
                }
            }
            List<Wire> wires = new ArrayList<>();
            Set<String> usedDeclvar = new HashSet<>();
            for (Wire w : g.wires()) {
                if (acRefUids.contains(w.to().node()) && NodeTypes.isVarRefPort(w.to().port())) {
                    continue;
                }
                wires.add(w);
                if (w.from().node().startsWith("declvar-")) {
                    usedDeclvar.add(w.from().node());
                }
            }
            List<NodeInstance> nodes = new ArrayList<>();
            for (NodeInstance n : g.nodes()) {
                if (n.uid().startsWith("declvar-") && !usedDeclvar.contains(n.uid())) {
                    continue;
                }
                if (acRefUids.contains(n.uid()) && n.options().containsKey(NodeTypes.VAR_REFS_OPTION)) {
                    Map<String, JsonElement> options = new LinkedHashMap<>(n.options());
                    options.remove(NodeTypes.VAR_REFS_OPTION);
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
