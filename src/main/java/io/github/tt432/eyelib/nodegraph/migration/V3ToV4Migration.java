package io.github.tt432.eyelib.nodegraph.migration;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * v3 → v4：RenderController 内联。
 */
public final class V3ToV4Migration implements VersionMigration {
    @Override
    public int fromVersion() {
        return 3;
    }

    @Override
    public GraphLibrary apply(GraphLibrary library) {
        return migrateV3ToV4(library);
    }

    /**
     * v3 → v4（仅 CLIENT_ENTITY 库）：
     * <ul>
     *   <li>rc.condition_entry 拆解：rc 线源（ref.rc）的 ref 直连 entity.root.render_controllers；
     *       condition 线/内联值移到该 ref.rc 的 condition 端口（同 ref 多条 entry 先者胜）；删 entry；</li>
     *   <li>entity.root geometries/textures/materials 上的声明线 → 重定向到主图第一个 ref.rc
     *       （uid 序）的同名声明端口；无 ref.rc → 断线（验证器 REF_NOT_CONNECTED 提示）；</li>
     *   <li>RENDER_CONTROLLER / ANIMATION_CONTROLLER 库不变（rc.root 新端口闲置）。</li>
     * </ul>
     */
    private static GraphLibrary migrateV3ToV4(GraphLibrary library) {
        if (library.kind() != GraphKind.CLIENT_ENTITY) {
            return new GraphLibrary(4, library.kind(), library.main(), library.graphs());
        }
        GraphData main = library.graphs().get(library.main());
        if (main == null) {
            return new GraphLibrary(4, library.kind(), library.main(), library.graphs());
        }
        Optional<NodeInstance> root = main.nodes().stream()
                .filter(n -> n.type().equals("entity.root")).findFirst();
        if (root.isEmpty()) {
            return new GraphLibrary(4, library.kind(), library.main(), library.graphs());
        }
        String rootUid = root.get().uid();

        List<NodeInstance> nodes = new ArrayList<>(main.nodes());
        List<Wire> wires = new ArrayList<>(main.wires());

        // 1. rc.condition_entry 拆解
        for (NodeInstance entry : main.nodes()) {
            if (!entry.type().equals("rc.condition_entry")) {
                continue;
            }
            // rc 线源
            String rcUid = null;
            for (Wire w : wires) {
                if (w.to().node().equals(entry.uid()) && w.to().port().equals("rc")) {
                    rcUid = w.from().node();
                    break;
                }
            }
            if (rcUid != null) {
                String rc = rcUid;
                boolean alreadyMounted = wires.stream().anyMatch(w ->
                        w.from().node().equals(rc) && w.from().port().equals("ref")
                                && w.to().node().equals(rootUid) && w.to().port().equals("render_controllers"));
                if (!alreadyMounted) {
                    wires.add(new Wire(new PortRef(rc, "ref"), new PortRef(rootUid, "render_controllers")));
                }
                // condition：线或内联值迁移（ref.rc 已有 condition 内容时先者胜）
                boolean refHasCondition = wires.stream().anyMatch(w ->
                        w.to().node().equals(rc) && w.to().port().equals("condition"));
                if (!refHasCondition) {
                    Optional<Wire> condWire = wires.stream().filter(w ->
                            w.to().node().equals(entry.uid()) && w.to().port().equals("condition")).findFirst();
                    if (condWire.isPresent()) {
                        wires.add(new Wire(condWire.get().from(), new PortRef(rc, "condition")));
                    } else if (entry.constants().containsKey("condition")) {
                        // 内联常量搬到 ref.rc（替换节点）
                        for (int i = 0; i < nodes.size(); i++) {
                            NodeInstance n = nodes.get(i);
                            if (n.uid().equals(rc) && !n.constants().containsKey("condition")) {
                                Map<String, JsonElement> constants = new LinkedHashMap<>(n.constants());
                                constants.put("condition", entry.constants().get("condition"));
                                nodes.set(i, new NodeInstance(n.uid(), n.type(), n.x(), n.y(),
                                        n.options(), Map.copyOf(constants)));
                            }
                        }
                    }
                }
            }
            // 删 entry 及其全部线
            nodes.removeIf(n -> n.uid().equals(entry.uid()));
            wires.removeIf(w -> w.from().node().equals(entry.uid()) || w.to().node().equals(entry.uid()));
        }

        // 2. entity.root 的 geo/tex/mat 声明线 → 重定向第一个 ref.rc
        String firstRc = nodes.stream().filter(n -> n.type().equals("ref.rc"))
                .map(NodeInstance::uid).min(String::compareTo).orElse(null);
        Set<String> legacyPorts = Set.of("geometries", "textures", "materials");
        List<Wire> retargeted = new ArrayList<>();
        for (Wire w : wires) {
            if (w.to().node().equals(rootUid) && legacyPorts.contains(w.to().port())) {
                if (firstRc != null) {
                    retargeted.add(new Wire(w.from(), new PortRef(firstRc, "decl_" + w.to().port())));
                } // 无 ref.rc → 断线
            } else {
                retargeted.add(w);
            }
        }
        wires = retargeted;

        Map<String, GraphData> graphs = new LinkedHashMap<>(library.graphs());
        graphs.put(library.main(), new GraphData(List.copyOf(nodes), List.copyOf(wires),
                main.variables(), main.placemats(), main.stickyNotes(), main.graphInterface()));
        return new GraphLibrary(4, library.kind(), library.main(), graphs);
    }
}
