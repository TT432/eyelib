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
 * v12 → v13：执行时机事件化（entity.root 槽 → event.* 源节点，state 端口翻转）。
 */
public final class V12ToV13Migration implements VersionMigration {
    @Override
    public int fromVersion() {
        return 12;
    }

    @Override
    public GraphLibrary apply(GraphLibrary library) {
        return migrateV12ToV13(library);
    }

    /**
     * v13 事件模型（规格 nodegraph-event-nodes，用户决策 2026-08-15）：
     * <ul>
     *   <li>entity.root 的 initialize/pre_animation/parent_setup EXEC IN 槽撤除，
     *       改为 event.* 源节点（仅 exec_out）：槽口连线（链尾 → 槽）改写为
     *       event 节点 → 链首 exec_in；无连线的槽不产节点；</li>
     *   <li>ac.state 的 on_entry/on_exit 从 IN 翻转为 OUT（端口 id 不变）：
     *       槽口连线（链尾 → state）改写为 state → 链首 exec_in。</li>
     * </ul>
     * 链内部拓扑（exec_out → exec_in 依序相连）不变，导出产物逐字节等价。
     */
    private static GraphLibrary migrateV12ToV13(GraphLibrary library) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            graphs.put(entry.getKey(), migrateEventsV13(entry.getValue()));
        }
        return new GraphLibrary(library.formatVersion(), library.kind(), library.main(), graphs);
    }

    private static GraphData migrateEventsV13(GraphData g) {
        Set<String> entityRootUids = new HashSet<>();
        Set<String> stateUids = new HashSet<>();
        for (NodeInstance n : g.nodes()) {
            if (NodeTypes.ENTITY_ROOT.id().equals(n.type())) {
                entityRootUids.add(n.uid());
            } else if (NodeTypes.AC_STATE.id().equals(n.type())) {
                stateUids.add(n.uid());
            }
        }
        // 旧槽口连线：(槽节点 uid, 槽端口) → 线；实体槽移植到 event 节点，state 槽翻转方向
        record OldSlotWire(Wire wire, boolean toEventNode) {
        }
        Map<String, OldSlotWire> slotWires = new LinkedHashMap<>();
        for (Wire w : g.wires()) {
            if (entityRootUids.contains(w.to().node())
                    && NodeTypes.EVENT_SCRIPT_SLOTS.containsValue(w.to().port())) {
                slotWires.put(w.to().node() + "#" + w.to().port(), new OldSlotWire(w, true));
            } else if (stateUids.contains(w.to().node())
                    && ("on_entry".equals(w.to().port()) || "on_exit".equals(w.to().port()))) {
                slotWires.put(w.to().node() + "#" + w.to().port(), new OldSlotWire(w, false));
            }
        }
        if (slotWires.isEmpty()) {
            return g;
        }
        Set<Wire> removed = new HashSet<>();
        List<Wire> added = new ArrayList<>();
        List<NodeInstance> newNodes = new ArrayList<>();
        Set<String> usedUids = new HashSet<>();
        for (NodeInstance n : g.nodes()) {
            usedUids.add(n.uid());
        }
        for (OldSlotWire slotWire : slotWires.values()) {
            Wire w = slotWire.wire();
            removed.add(w);
            // 沿 exec_in 反走找链首
            String head = w.from().node();
            while (true) {
                String prev = null;
                for (Wire back : g.wires()) {
                    if (back.to().node().equals(head) && "exec_in".equals(back.to().port())) {
                        prev = back.from().node();
                        break;
                    }
                }
                if (prev == null) {
                    break;
                }
                head = prev;
            }
            NodeInstance headNode = null;
            for (NodeInstance n : g.nodes()) {
                if (n.uid().equals(head)) {
                    headNode = n;
                    break;
                }
            }
            float x = headNode != null ? headNode.x() - 280 : 0;
            float y = headNode != null ? headNode.y() : 0;
            String sourceUid;
            String sourcePort;
            if (slotWire.toEventNode()) {
                sourceUid = uniqueUid("event_" + w.to().port(), usedUids);
                sourcePort = "exec_out";
                newNodes.add(new NodeInstance(sourceUid, "event." + w.to().port(), x, y,
                        Map.of(), Map.of()));
            } else {
                sourceUid = w.to().node();
                sourcePort = w.to().port();
            }
            added.add(new Wire(new PortRef(sourceUid, sourcePort), new PortRef(head, "exec_in")));
        }
        List<Wire> wires = new ArrayList<>();
        for (Wire w : g.wires()) {
            if (!removed.contains(w)) {
                wires.add(w);
            }
        }
        wires.addAll(added);
        List<NodeInstance> nodes = new ArrayList<>(g.nodes());
        nodes.addAll(newNodes);
        return new GraphData(nodes, wires, g.variables(), g.placemats(), g.stickyNotes(),
                g.graphInterface());
    }

    private static String uniqueUid(String base, Set<String> used) {
        String uid = base;
        int seq = 2;
        while (used.contains(uid)) {
            uid = base + "_" + (seq++);
        }
        used.add(uid);
        return uid;
    }
}
