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
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/**
 * v10 → v11：变长 call 参数列表化（argN → args 选项）。
 */
public final class V10ToV11Migration implements VersionMigration {
    @Override
    public int fromVersion() {
        return 10;
    }

    @Override
    public GraphLibrary apply(GraphLibrary library) {
        return migrateV10ToV11(library);
    }

    /**
     * v11 变长参数列表化（规格 nodegraph-variadic-call-list，用户决策 2026-08-10）：
     * <ul>
     *   <li>arg_count 选项删除（定长由签名决定、变长由列表长度决定）；</li>
     *   <li>变长/未知函数的变长尾参 argN（序号 > 固定前缀长）的行内常量/const 连线
     *       收进 args 列表选项；连线源为非常量表达式的无法列表化，丢弃（实机普查零出现）；</li>
     *   <li>仅喂被收端口的孤儿 const 节点一并删除。</li>
     * </ul>
     */
    private static GraphLibrary migrateV10ToV11(GraphLibrary library) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            graphs.put(entry.getKey(), migrateCallArgsV11(entry.getValue()));
        }
        return new GraphLibrary(library.formatVersion(), library.kind(), library.main(), graphs);
    }

    private static GraphData migrateCallArgsV11(GraphData g) {
        record HarvestedPort(String uid, String port) {
        }
        List<NodeInstance> nodes = new ArrayList<>();
        Set<HarvestedPort> harvested = new HashSet<>();
        boolean any = false;
        for (NodeInstance n : g.nodes()) {
            if (!isCallNode(n.type())) {
                nodes.add(n);
                continue;
            }
            any = true;
            String function = n.optionString("function", "");
            var sig = io.github.tt432.eyelib.nodegraph.MolangFunctionSignatures.find(function);
            int fixed = sig != null ? sig.fixed().size() : 0;
            boolean variadic = sig == null || sig.varArg() != null;
            Map<String, JsonElement> options = new LinkedHashMap<>(n.options());
            options.remove("arg_count");
            Map<String, JsonElement> constants = new LinkedHashMap<>(n.constants());
            if (variadic) {
                // 按端口序号收集变长尾参值：先常量，后连线 const（非常量源丢弃）
                Map<Integer, JsonElement> byIndex = new TreeMap<>();
                for (var ce : constants.entrySet()) {
                    Integer idx = argIndex(ce.getKey());
                    if (idx != null && idx > fixed) {
                        byIndex.put(idx, ce.getValue());
                    }
                }
                for (Wire w : g.wires()) {
                    if (!w.to().node().equals(n.uid())) {
                        continue;
                    }
                    Integer idx = argIndex(w.to().port());
                    if (idx == null || idx <= fixed || byIndex.containsKey(idx)) {
                        continue;
                    }
                    NodeInstance src = g.findNode(w.from().node()).orElse(null);
                    if (src != null && src.type().startsWith("const.")
                            && src.options().containsKey("value")) {
                        byIndex.put(idx, src.options().get("value"));
                    }
                }
                for (int idx : byIndex.keySet()) {
                    constants.remove("arg" + idx);
                    harvested.add(new HarvestedPort(n.uid(), "arg" + idx));
                }
                if (!byIndex.isEmpty()) {
                    com.google.gson.JsonArray args = new com.google.gson.JsonArray();
                    byIndex.values().forEach(args::add);
                    options.put("args", args);
                }
            }
            nodes.add(new NodeInstance(n.uid(), n.type(), n.x(), n.y(), options, constants));
        }
        if (!any) {
            return g;
        }
        // 线清理：删掉落入被收端口的线；孤儿 const（只喂被收端口）一并删除
        List<Wire> wires = new ArrayList<>();
        Set<String> constCandidates = new HashSet<>();
        for (Wire w : g.wires()) {
            if (harvested.contains(new HarvestedPort(w.to().node(), w.to().port()))) {
                constCandidates.add(w.from().node());
                continue;
            }
            wires.add(w);
        }
        if (!constCandidates.isEmpty()) {
            Set<String> stillUsed = new HashSet<>();
            for (Wire w : wires) {
                stillUsed.add(w.from().node());
            }
            constCandidates.removeAll(stillUsed);
            if (!constCandidates.isEmpty()) {
                nodes.removeIf(n -> constCandidates.contains(n.uid()) && n.type().startsWith("const."));
            }
        }
        return new GraphData(nodes, wires, g.variables(), g.placemats(), g.stickyNotes(),
                g.graphInterface());
    }

    private static boolean isCallNode(String type) {
        return NodeTypes.QUERY_CALL.id().equals(type) || NodeTypes.MATH_CALL.id().equals(type)
                || NodeTypes.EXEC_CALL.id().equals(type);
    }

    /** "argN" → N；非 argN 形态 → null。 */
    private static @Nullable Integer argIndex(String port) {
        if (!port.startsWith("arg") || port.length() <= 3) {
            return null;
        }
        try {
            return Integer.parseInt(port.substring(3));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
