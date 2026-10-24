package io.github.tt432.eyelib.nodegraph.migration;

import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphKind;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * v2 → v3：声明连线化（CLIENT_ENTITY 库）。
 */
public final class V2ToV3Migration implements VersionMigration {
    @Override
    public int fromVersion() {
        return 2;
    }

    @Override
    public GraphLibrary apply(GraphLibrary library) {
        return migrateV2ToV3(library);
    }

    private static GraphLibrary migrateV2ToV3(GraphLibrary library) {
        if (library.kind() != GraphKind.CLIENT_ENTITY) {
            return library;
        }
        GraphData main = library.graphs().get(library.main());
        if (main == null) {
            return library;
        }
        Optional<NodeInstance> root = main.nodes().stream()
                .filter(n -> n.type().equals("entity.root")).findFirst();
        if (root.isEmpty()) {
            return library; // 无主图根锚点：验证器 ROOT_COUNT 已报，迁移不做猜测
        }
        String rootUid = root.get().uid();

        List<NodeInstance> mainNodes = new ArrayList<>(main.nodes());
        List<Wire> mainWires = new ArrayList<>(main.wires());
        Set<String> uidTaken = new HashSet<>();
        for (NodeInstance node : mainNodes) {
            uidTaken.add(node.uid());
        }
        int[] counter = {0};

        // 主图：无声明连线的 ref.{geometry,texture,material,animation,ac} → 补线到对应声明端口
        for (NodeInstance node : main.nodes()) {
            String port = NodeTypes.DECLARATION_PORTS.get(node.type());
            if (port == null) {
                continue;
            }
            boolean wired = mainWires.stream().anyMatch(w ->
                    w.from().node().equals(node.uid()) && w.to().node().equals(rootUid)
                            && w.to().port().equals(port));
            if (!wired) {
                mainWires.add(new Wire(new PortRef(node.uid(), "ref"), new PortRef(rootUid, port)));
            }
        }

        // 主图：未连任何 rc.condition_entry 的 ref.rc → 新建条目（置于 ref 右侧，condition 端口默认 1）
        for (NodeInstance node : main.nodes()) {
            if (!node.type().equals("ref.rc")) {
                continue;
            }
            boolean connected = mainWires.stream().anyMatch(w ->
                    w.from().node().equals(node.uid()) && w.to().port().equals("rc")
                            && main.findNode(w.to().node())
                            .map(n -> n.type().equals("rc.condition_entry")).orElse(false));
            if (connected) {
                continue;
            }
            String entryUid = MigrationSupport.freshUid(uidTaken, counter);
            mainNodes.add(new NodeInstance(entryUid, "rc.condition_entry",
                    node.x() + 240, node.y(), Map.of(), Map.of()));
            mainWires.add(new Wire(new PortRef(node.uid(), "ref"), new PortRef(entryUid, "rc")));
            mainWires.add(new Wire(new PortRef(entryUid, "entry"), new PortRef(rootUid, "render_controllers")));
        }

        // 子图：完全无连线的声明类 ref → 移到主图（uid/选项保留，置于主图空闲区）并补线
        List<NodeInstance> moved = new ArrayList<>();
        Map<String, GraphData> graphs = new LinkedHashMap<>(library.graphs());
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            if (entry.getKey().equals(library.main())) {
                continue;
            }
            GraphData graph = entry.getValue();
            Set<String> wiredUids = new HashSet<>();
            for (Wire wire : graph.wires()) {
                wiredUids.add(wire.from().node());
                wiredUids.add(wire.to().node());
            }
            List<NodeInstance> remaining = new ArrayList<>();
            boolean changed = false;
            for (NodeInstance node : graph.nodes()) {
                if (NodeTypes.DECLARATION_PORTS.containsKey(node.type()) && !wiredUids.contains(node.uid())) {
                    moved.add(node);
                    changed = true;
                } else {
                    remaining.add(node);
                }
            }
            if (changed) {
                graphs.put(entry.getKey(), new GraphData(List.copyOf(remaining), graph.wires(),
                        graph.variables(), graph.placemats(), graph.stickyNotes(), graph.graphInterface()));
            }
        }
        if (!moved.isEmpty()) {
            // 主图空闲区：现有节点最下方起，向左对齐主图最小 x，纵排
            float minX = (float) mainNodes.stream().mapToDouble(NodeInstance::x).min().orElse(0);
            float y = (float) mainNodes.stream().mapToDouble(NodeInstance::y).max().orElse(0) + 160;
            for (NodeInstance node : moved) {
                String uid = uidTaken.contains(node.uid()) ? MigrationSupport.freshUid(uidTaken, counter) : node.uid();
                uidTaken.add(uid);
                mainNodes.add(new NodeInstance(uid, node.type(), minX, y,
                        node.options(), node.constants()));
                mainWires.add(new Wire(new PortRef(uid, "ref"),
                        // moved 由上方 DECLARATION_PORTS.containsKey 过滤，get 必中
                        new PortRef(rootUid, Objects.requireNonNull(NodeTypes.DECLARATION_PORTS.get(node.type())))));
                y += 140;
            }
        }

        graphs.put(library.main(), new GraphData(List.copyOf(mainNodes), List.copyOf(mainWires),
                main.variables(), main.placemats(), main.stickyNotes(), main.graphInterface()));
        return new GraphLibrary(3, library.kind(), library.main(), graphs);
    }
}
