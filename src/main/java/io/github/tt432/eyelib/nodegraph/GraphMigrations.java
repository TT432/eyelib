package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 图文档迁移：按 format_version 链式执行（规格 nodegraph-eproject-variables §3.4、
 * nodegraph-declaration-wiring §2.4）。
 *
 * <p>v1 → v2 变量节点化（无兼容双轨）：
 * <ul>
 *   <li>{@code var.get}（name=variable.foo）→ {@code variable}（name=foo，不带根）；</li>
 *   <li>{@code exec.set_var} root=variable → 新 {@code exec.set_var}（target 引脚）
 *       + 自动生成 variable 节点（置于原节点左上）连线 target；</li>
 *   <li>{@code exec.set_var} root=temp → {@code exec.set_temp}（name 保留）。</li>
 * </ul>
 *
 * <p>v2 → v3 声明连线化（CLIENT_ENTITY 库；声明 = 连线，扫描语义废止）：
 * <ul>
 *   <li>主图无声明连线的 ref.{geometry,texture,material,animation,ac} → 补线到 entity.root
 *       对应声明端口；</li>
 *   <li>主图未连任何 rc.condition_entry 的 ref.rc → 新建 rc.condition_entry（置于 ref 右侧，
 *       condition 用端口默认 1），ref→entry.rc、entry.entry→root.render_controllers；</li>
 *   <li>子图中完全无连线的声明类 ref → 移到主图（uid/选项保留，置于主图空闲区）并补线；
 *       有连线的不动（验证器 REF_NOT_CONNECTED 提示）；</li>
 *   <li>RENDER_CONTROLLER / ANIMATION_CONTROLLER 库不变。</li>
 * </ul>
 *
 * <p>纯函数：输入输出均为不可变文档；加载路径（资源包 loader / EprojectIo）统一调用。
 * 已是新格式的文档原样返回。
 */
public final class GraphMigrations {
    private GraphMigrations() {
    }

    /** 迁移整个图库；format_version 升到 {@link GraphLibrary#CURRENT_FORMAT_VERSION}。 */
    public static GraphLibrary migrate(GraphLibrary library) {
        GraphLibrary result = library;
        if (result.formatVersion() < 2) {
            result = migrateV1ToV2(result);
        }
        if (result.formatVersion() < 3) {
            result = migrateV2ToV3(result);
        }
        if (result.formatVersion() < 4) {
            result = migrateV3ToV4(result);
        }
        return new GraphLibrary(GraphLibrary.CURRENT_FORMAT_VERSION, result.kind(), result.main(),
                result.graphs());
    }

    // ---------- v1 → v2：变量节点化 ----------

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
                    String name = optionString(node, "name", "");
                    nodes.add(new NodeInstance(node.uid(), "variable", node.x(), node.y(),
                            Map.of("name", new JsonPrimitive(stripRoot(name, "variable"))),
                            node.constants()));
                }
                case "exec.set_var" -> {
                    if (!node.options().containsKey("name")) {
                        // 新格式（target 引脚、无选项）原样保留
                        nodes.add(node);
                        break;
                    }
                    String root = optionString(node, "root", "variable");
                    String name = optionString(node, "name", "");
                    if ("temp".equals(root)) {
                        nodes.add(new NodeInstance(node.uid(), "exec.set_temp", node.x(), node.y(),
                                Map.of("name", new JsonPrimitive(name)), node.constants()));
                    } else {
                        // 新 exec.set_var（无选项）+ 自动 variable 节点连线 target
                        String varUid = freshUid(uidTaken, counter);
                        nodes.add(new NodeInstance(node.uid(), "exec.set_var", node.x(), node.y(),
                                Map.of(), node.constants()));
                        nodes.add(new NodeInstance(varUid, "variable", node.x() - 180, node.y() + 30,
                                Map.of("name", new JsonPrimitive(stripRoot(name, "variable"))), Map.of()));
                        wires.add(new Wire(new PortRef(varUid, "out"), new PortRef(node.uid(), "target")));
                    }
                }
                default -> nodes.add(node);
            }
        }
        return new GraphData(List.copyOf(nodes), List.copyOf(wires), graph.variables(),
                graph.placemats(), graph.stickyNotes(), graph.graphInterface());
    }

    // ---------- v2 → v3：声明连线化（规格 §2.4） ----------

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
            String entryUid = freshUid(uidTaken, counter);
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
                String uid = uidTaken.contains(node.uid()) ? freshUid(uidTaken, counter) : node.uid();
                uidTaken.add(uid);
                mainNodes.add(new NodeInstance(uid, node.type(), minX, y,
                        node.options(), node.constants()));
                mainWires.add(new Wire(new PortRef(uid, "ref"),
                        new PortRef(rootUid, NodeTypes.DECLARATION_PORTS.get(node.type()))));
                y += 140;
            }
        }

        graphs.put(library.main(), new GraphData(List.copyOf(mainNodes), List.copyOf(mainWires),
                main.variables(), main.placemats(), main.stickyNotes(), main.graphInterface()));
        return new GraphLibrary(3, library.kind(), library.main(), graphs);
    }

    // ---------- v3 → v4：RenderController 内联（规格 nodegraph-inline-render-controller §5） ----------

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

    private static String optionString(NodeInstance node, String id, String fallback) {
        JsonElement value = node.options().get(id);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : fallback;
    }

    private static String stripRoot(String name, String root) {
        String prefix = root + ".";
        return name.startsWith(prefix) ? name.substring(prefix.length()) : name;
    }

    private static String freshUid(Set<String> taken, int[] counter) {
        String uid;
        do {
            uid = "mig" + counter[0]++;
        } while (taken.contains(uid));
        taken.add(uid);
        return uid;
    }
}
