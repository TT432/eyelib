package io.github.tt432.eyelib.nodegraph.decompile;

import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.StickyNote;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 分层自动布局（规格 nodegraph-workbench §W2）：<b>到汇最长路径定 x 层、汇出发反向 DFS 序定 y</b>。
 *
 * <p>坐标遵循编辑器既有约定（versions/1.20.1/run/config/eyelib/nodegraph/ng_smoke.json）：
 * 数据流方向 x 左→右（生产者左、消费者右，汇——装配根——在最右列），列距 300、行距 110。
 * 无连线的孤立节点（如声明表 ref.*）放在汇左一列。纯函数、确定性：同输入同输出；
 * 环只做防御（层按 0 计），环本身由 GraphValidator.CYCLE 报告。
 */
public final class GraphLayout {
    private GraphLayout() {
    }

    /** 列距（同 ng_smoke.json 的 300）。 */
    public static final float X_SPACING = 300f;
    /** 行距。 */
    public static final float Y_SPACING = 110f;

    /**
     * 分层布局：返回坐标重排后的节点列表（其余字段直通，顺序与输入一致）。
     *
     * <p>层定义：汇（无出线的节点）为 0 层；其余节点 = 1 + 消费者层最大值（到汇最长路径）。
     * x = (maxLayer − layer) × {@link #X_SPACING}（汇在最右）；同层节点按「汇出发沿连线反向
     * DFS 的先序序号」排序，y = 层内序号 × {@link #Y_SPACING}。
     */
    public static List<NodeInstance> layout(List<NodeInstance> nodes, List<Wire> wires) {
        Map<String, List<String>> consumers = new HashMap<>();
        Map<String, List<Wire>> producers = new HashMap<>();
        Set<String> wiredNodes = new HashSet<>();
        for (Wire w : wires) {
            consumers.computeIfAbsent(w.from().node(), k -> new ArrayList<>()).add(w.to().node());
            producers.computeIfAbsent(w.to().node(), k -> new ArrayList<>()).add(w);
            wiredNodes.add(w.from().node());
            wiredNodes.add(w.to().node());
        }
        consumers.values().forEach(list -> list.sort(Comparator.naturalOrder()));
        producers.values().forEach(list -> list.sort(
                Comparator.comparing((Wire w) -> w.from().node()).thenComparing(w -> w.from().port())));

        // x 层：到汇最长路径（记忆化 DFS；环防御 → 0）
        Map<String, Integer> layers = new HashMap<>();
        int maxLayerValue = 0;
        for (NodeInstance node : nodes) {
            if (wiredNodes.contains(node.uid())) {
                int layer = layerOf(node.uid(), consumers, layers, new HashSet<>());
                maxLayerValue = Math.max(maxLayerValue, layer);
            }
        }
        final int maxLayer = maxLayerValue;
        // 孤立节点（无任何连线，如声明表 ref.*）：汇左一列（无汇时同列）
        int isolatedLayer = maxLayer > 0 ? 1 : 0;

        // y 序：汇（uid 字典序）出发，沿连线反向 DFS 的先序序号
        Map<String, Integer> order = new HashMap<>();
        List<String> sinks = new ArrayList<>();
        for (NodeInstance node : nodes) {
            if (!consumers.containsKey(node.uid())) {
                sinks.add(node.uid());
            }
        }
        sinks.sort(Comparator.naturalOrder());
        Set<String> visited = new HashSet<>();
        int[] seq = {0};
        for (String sink : sinks) {
            dfsOrder(sink, producers, visited, order, seq);
        }

        // 同层分组 → 按 DFS 序定 y
        Map<Integer, List<NodeInstance>> byLayer = new HashMap<>();
        for (NodeInstance node : nodes) {
            int layer = wiredNodes.contains(node.uid()) ? layers.get(node.uid()) : isolatedLayer;
            byLayer.computeIfAbsent(layer, k -> new ArrayList<>()).add(node);
        }
        Map<String, Float> xs = new HashMap<>();
        Map<String, Float> ys = new HashMap<>();
        byLayer.forEach((layer, group) -> {
            group.sort(Comparator
                    .comparingInt((NodeInstance n) -> order.getOrDefault(n.uid(), Integer.MAX_VALUE))
                    .thenComparing(NodeInstance::uid));
            float x = (maxLayer - layer) * X_SPACING;
            for (int i = 0; i < group.size(); i++) {
                xs.put(group.get(i).uid(), x);
                ys.put(group.get(i).uid(), i * Y_SPACING);
            }
        });

        List<NodeInstance> out = new ArrayList<>(nodes.size());
        for (NodeInstance n : nodes) {
            out.add(new NodeInstance(n.uid(), n.type(),
                    xs.getOrDefault(n.uid(), 0f), ys.getOrDefault(n.uid(), 0f),
                    n.options(), n.constants()));
        }
        return out;
    }

    /** 便签定位：图最右列右侧一列，自上而下堆叠（其余字段直通）。 */
    public static List<StickyNote> placeStickyNotes(List<StickyNote> notes, List<NodeInstance> laidOutNodes) {
        float maxX = 0;
        for (NodeInstance n : laidOutNodes) {
            maxX = Math.max(maxX, n.x());
        }
        List<StickyNote> out = new ArrayList<>(notes.size());
        for (int i = 0; i < notes.size(); i++) {
            StickyNote n = notes.get(i);
            out.add(new StickyNote(n.uid(), n.text(), maxX + X_SPACING, i * Y_SPACING,
                    n.width(), n.height(), n.color()));
        }
        return out;
    }

    private static int layerOf(String uid, Map<String, List<String>> consumers,
                               Map<String, Integer> cache, Set<String> visiting) {
        Integer cached = cache.get(uid);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(uid)) {
            return 0; // 环防御：不挂死，环由 GraphValidator.CYCLE 报告
        }
        int layer = 0;
        for (String consumer : consumers.getOrDefault(uid, List.of())) {
            layer = Math.max(layer, layerOf(consumer, consumers, cache, visiting) + 1);
        }
        visiting.remove(uid);
        cache.put(uid, layer);
        return layer;
    }

    private static void dfsOrder(String uid, Map<String, List<Wire>> producers,
                                 Set<String> visited, Map<String, Integer> order, int[] seq) {
        if (!visited.add(uid)) {
            return;
        }
        order.put(uid, seq[0]++);
        for (Wire w : producers.getOrDefault(uid, List.of())) {
            dfsOrder(w.from().node(), producers, visited, order, seq);
        }
    }
}
