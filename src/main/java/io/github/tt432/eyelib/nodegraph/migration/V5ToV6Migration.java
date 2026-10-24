package io.github.tt432.eyelib.nodegraph.migration;

import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphKind;
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
 * v5 → v6：有序条目链 + rc.root decl_* 连线移除。
 */
public final class V5ToV6Migration implements VersionMigration {
    @Override
    public int fromVersion() {
        return 5;
    }

    @Override
    public GraphLibrary apply(GraphLibrary library) {
        return migrateV5ToV6(library);
    }

    private static final Set<String> V6_LIST_PORTS = Set.of("textures", "materials", "part_visibility");
    private static final Set<String> V6_DECL_PORTS = Set.of(
            "decl_geometries", "decl_textures", "decl_materials");

    private static GraphLibrary migrateV5ToV6(GraphLibrary library) {
        if (library.kind() != GraphKind.CLIENT_ENTITY && library.kind() != GraphKind.RENDER_CONTROLLER) {
            return new GraphLibrary(6, library.kind(), library.main(), library.graphs());
        }
        GraphData main = library.graphs().get(library.main());
        if (main == null) {
            return new GraphLibrary(6, library.kind(), library.main(), library.graphs());
        }
        Set<String> rcRoots = new HashSet<>();
        for (NodeInstance n : main.nodes()) {
            if (n.type().equals("rc.root")) {
                rcRoots.add(n.uid());
            }
        }
        if (rcRoots.isEmpty()) {
            return new GraphLibrary(6, library.kind(), library.main(), library.graphs());
        }
        List<Wire> wires = new ArrayList<>(main.wires());
        // decl_* 连线删除（ref 节点保留；协议短名由 DeclarationTables carve-out 接管）
        wires.removeIf(w -> rcRoots.contains(w.to().node()) && V6_DECL_PORTS.contains(w.to().port()));
        // 多条目线 → 链（uid 序 = v5 发射序，产物等价）
        for (String rcUid : rcRoots.stream().sorted().toList()) {
            for (String port : V6_LIST_PORTS) {
                List<Wire> direct = wires.stream()
                        .filter(w -> w.to().node().equals(rcUid) && w.to().port().equals(port))
                        .sorted((a, b) -> a.from().node().compareTo(b.from().node()))
                        .toList();
                if (direct.size() <= 1) {
                    continue;
                }
                wires.removeAll(direct);
                wires.add(direct.get(0));
                for (int i = 1; i < direct.size(); i++) {
                    wires.add(new Wire(direct.get(i).from(),
                            new PortRef(direct.get(i - 1).from().node(), "next")));
                }
            }
        }
        Map<String, GraphData> graphs = new LinkedHashMap<>(library.graphs());
        graphs.put(library.main(), new GraphData(main.nodes(), List.copyOf(wires),
                main.variables(), main.placemats(), main.stickyNotes(), main.graphInterface()));
        return new GraphLibrary(6, library.kind(), library.main(), graphs);
    }
}
