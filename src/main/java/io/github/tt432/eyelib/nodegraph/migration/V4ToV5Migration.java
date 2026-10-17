package io.github.tt432.eyelib.nodegraph.migration;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.ColorValues;
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
 * v4 → v5：rc.root 颜色通道端口化（COLOR 端口）。
 */
public final class V4ToV5Migration implements VersionMigration {
    @Override
    public int fromVersion() {
        return 4;
    }

    @Override
    public GraphLibrary apply(GraphLibrary library) {
        return migrateV4ToV5(library);
    }

    /**
     * v4 → v5（规格 §6.5）：rc.root 16 个 float 颜色通道端口 → 4 个 COLOR 端口。
     *
     * <ul>
     *   <li>四通道均无无线无常数 → 不迁移该字段（新端口留空 = 字段不输出，同语义）；</li>
     *   <li>四通道均无连线且常数（缺省 1）全部 8bit 精确 → const.color（取色器节点）接线；</li>
     *   <li>否则 → color.compose：通道线/内联常数原样搬到 r/g/b/a，compose 输出接颜色端口；</li>
     *   <li>所有库类型都迁移（rc.root 存在于 RENDER_CONTROLLER 与 CLIENT_ENTITY 库）。</li>
     * </ul>
     */
    private static GraphLibrary migrateV4ToV5(GraphLibrary library) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            graphs.put(entry.getKey(), migrateColorsV5(entry.getValue()));
        }
        return new GraphLibrary(5, library.kind(), library.main(), graphs);
    }

    /** (字段端口, 通道前缀) 四组颜色。 */
    private static final String[][] COLOR_FIELDS = {
            {"color", "color"}, {"is_hurt_color", "is_hurt"},
            {"on_fire_color", "on_fire"}, {"overlay_color", "overlay"}};
    private static final String[] CHANNELS = {"r", "g", "b", "a"};

    private static GraphData migrateColorsV5(GraphData graph) {
        List<NodeInstance> nodes = new ArrayList<>(graph.nodes());
        List<Wire> wires = new ArrayList<>(graph.wires());
        Set<String> uidTaken = new HashSet<>();
        for (NodeInstance n : nodes) {
            uidTaken.add(n.uid());
        }
        int[] counter = {0};

        for (int i = 0; i < nodes.size(); i++) {
            NodeInstance rc = nodes.get(i);
            if (!rc.type().equals("rc.root")) {
                continue;
            }
            Map<String, JsonElement> rcConstants = new LinkedHashMap<>(rc.constants());
            int created = 0;
            for (String[] field : COLOR_FIELDS) {
                String fieldPort = field[0];
                String[] channelPorts = new String[4];
                for (int c = 0; c < 4; c++) {
                    channelPorts[c] = field[1] + "_" + CHANNELS[c];
                }
                // 收集每通道的线与内联常数
                Wire[] channelWires = new Wire[4];
                JsonElement[] channelConstants = new JsonElement[4];
                boolean anyContent = false;
                boolean anyWire = false;
                boolean allByteExact = true;
                for (int c = 0; c < 4; c++) {
                    String cp = channelPorts[c];
                    int idx = c;
                    channelWires[c] = wires.stream()
                            .filter(w -> w.to().node().equals(rc.uid()) && w.to().port().equals(cp))
                            .findFirst().orElse(null);
                    channelConstants[c] = rcConstants.get(cp);
                    if (channelWires[idx] != null) {
                        anyWire = true;
                        anyContent = true;
                    }
                    if (channelConstants[idx] != null) {
                        anyContent = true;
                        JsonElement cc = channelConstants[idx];
                        if (!(cc instanceof JsonPrimitive p) || !p.isNumber()
                                || !ColorValues.isByteExact(p.getAsDouble())) {
                            allByteExact = false;
                        }
                    }
                }
                if (!anyContent) {
                    continue;
                }
                // 创建 const.color / color.compose
                String newUid = MigrationSupport.freshUid(uidTaken, counter);
                uidTaken.add(newUid);
                NodeInstance created_;
                if (!anyWire && allByteExact) {
                    float[] values = new float[4];
                    for (int c = 0; c < 4; c++) {
                        values[c] = channelConstants[c] != null
                                ? (float) channelConstants[c].getAsDouble() : 1f;
                    }
                    created_ = new NodeInstance(newUid, "const.color",
                            rc.x() + 60, rc.y() + 40 * (created++),
                            Map.of("value", new JsonPrimitive(
                                    ColorValues.toHex(values[0], values[1], values[2], values[3]))),
                            Map.of());
                } else {
                    Map<String, JsonElement> composeConstants = new LinkedHashMap<>();
                    for (int c = 0; c < 4; c++) {
                        if (channelConstants[c] != null) {
                            composeConstants.put(CHANNELS[c], channelConstants[c]);
                        }
                    }
                    created_ = new NodeInstance(newUid, "color.compose",
                            rc.x() + 60, rc.y() + 40 * (created++),
                            Map.of(), Map.copyOf(composeConstants));
                    for (int c = 0; c < 4; c++) {
                        if (channelWires[c] != null) {
                            wires.add(new Wire(channelWires[c].from(), new PortRef(newUid, CHANNELS[c])));
                        }
                    }
                }
                nodes.add(created_);
                wires.add(new Wire(new PortRef(newUid, "out"), new PortRef(rc.uid(), fieldPort)));
                // 拆旧：通道线与通道常数
                for (String cp : channelPorts) {
                    wires.removeIf(w -> w.to().node().equals(rc.uid()) && w.to().port().equals(cp));
                    rcConstants.remove(cp);
                }
            }
            if (!rcConstants.equals(rc.constants())) {
                nodes.set(i, new NodeInstance(rc.uid(), rc.type(), rc.x(), rc.y(),
                        rc.options(), Map.copyOf(rcConstants)));
            }
        }
        return new GraphData(List.copyOf(nodes), List.copyOf(wires),
                graph.variables(), graph.placemats(), graph.stickyNotes(), graph.graphInterface());
    }
}
