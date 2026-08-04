package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 图文档迁移：format_version 1 → 2（规格 nodegraph-eproject-variables §3.4）。
 *
 * <p>变量节点化的一次性迁移（无兼容双轨）：
 * <ul>
 *   <li>{@code var.get}（name=variable.foo）→ {@code variable}（name=foo，不带根）；</li>
 *   <li>{@code exec.set_var} root=variable → 新 {@code exec.set_var}（target 引脚）
 *       + 自动生成 variable 节点（置于原节点左上）连线 target；</li>
 *   <li>{@code exec.set_var} root=temp → {@code exec.set_temp}（name 保留）。</li>
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
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            graphs.put(entry.getKey(), migrateGraph(entry.getValue()));
        }
        return new GraphLibrary(GraphLibrary.CURRENT_FORMAT_VERSION, library.kind(), library.main(), graphs);
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
