package io.github.tt432.eyelib.nodegraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 图可达性与环检测：值/exec 边成环、反向可达（根锚点侧）、正向可达（时机源侧）。
 *
 * <p>纯静态工具，只对传入的边列表做分析，不持有图状态。
 */
final class GraphReachability {

    private GraphReachability() {
    }

    /** 检查 9：值边 + exec 边上的环（SLOT 边不参与）。 */
    static void detectValueCycle(Map<String, NodeInstance> byUid, List<Wire> edges, List<Diagnostic> out) {
        Map<String, List<String>> adj = new HashMap<>();
        for (Wire wire : edges) {
            adj.computeIfAbsent(wire.from().node(), k -> new ArrayList<>()).add(wire.to().node());
        }
        Map<String, Integer> color = new HashMap<>();
        for (String uid : byUid.keySet()) {
            if (color.getOrDefault(uid, 0) == 0) {
                detectValueCycleDfs(uid, adj, color, out);
            }
        }
    }

    private static void detectValueCycleDfs(String start, Map<String, List<String>> adj,
                                            Map<String, Integer> color, List<Diagnostic> out) {
        record Frame(String uid, int next) {
        }
        Deque<Frame> frames = new ArrayDeque<>();
        color.put(start, 1);
        frames.push(new Frame(start, 0));
        while (!frames.isEmpty()) {
            Frame top = frames.peek();
            List<String> neighbors = adj.getOrDefault(top.uid(), List.of());
            if (top.next() < neighbors.size()) {
                frames.pop();
                frames.push(new Frame(top.uid(), top.next() + 1));
                String v = neighbors.get(top.next());
                int c = color.getOrDefault(v, 0);
                if (c == 1) {
                    out.add(Diagnostic.error(GraphValidator.CYCLE, "数据流/执行流存在环，途经节点：" + v, v));
                } else if (c == 0) {
                    color.put(v, 1);
                    frames.push(new Frame(v, 0));
                }
            } else {
                frames.pop();
                color.put(top.uid(), 2);
            }
        }
    }

    /** 从根锚点沿边反向（to → from）可达的节点集合。 */
    static Set<String> reverseReachable(Set<String> roots, List<Wire> edges) {
        Map<String, List<String>> reverse = new HashMap<>();
        for (Wire wire : edges) {
            reverse.computeIfAbsent(wire.to().node(), k -> new ArrayList<>()).add(wire.from().node());
        }
        Set<String> visited = new LinkedHashSet<>(roots);
        Deque<String> queue = new ArrayDeque<>(roots);
        while (!queue.isEmpty()) {
            String uid = queue.poll();
            for (String prev : reverse.getOrDefault(uid, List.of())) {
                if (visited.add(prev)) {
                    queue.add(prev);
                }
            }
        }
        return visited;
    }

    /** 从时机源锚点沿边正向（from → to）可达的节点集合（含种子自身）。 */
    static Set<String> forwardReachable(Set<String> sources, List<Wire> edges) {
        Map<String, List<String>> forward = new HashMap<>();
        for (Wire wire : edges) {
            forward.computeIfAbsent(wire.from().node(), k -> new ArrayList<>()).add(wire.to().node());
        }
        Set<String> visited = new LinkedHashSet<>(sources);
        Deque<String> queue = new ArrayDeque<>(sources);
        while (!queue.isEmpty()) {
            String uid = queue.poll();
            for (String next : forward.getOrDefault(uid, List.of())) {
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return visited;
    }
}
