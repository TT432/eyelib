package io.github.tt432.eyelib.nodegraph;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * v6 有序条目链校验：列表端口多头、next 链成环、条目孤儿。
 *
 * <p>纯静态工具，只通过参数与 {@link GraphValidator} 的诊断码常量交互。
 */
final class EntryChainValidator {

    private EntryChainValidator() {
    }

    /** v6 三类有序条目类型。 */
    private static final Set<String> CHAIN_ENTRY_TYPES = Set.of(
            "list.entry", "material.entry", "part_visibility.entry");
    /** rc.root 列表端口 → 链头条目类型。 */
    private static final Map<String, String> CHAIN_HEAD_OF_PORT = Map.of(
            "textures", "list.entry",
            "materials", "material.entry",
            "part_visibility", "part_visibility.entry");

    /**
     * 检查 23（ERROR/WARNING，CLIENT_ENTITY 与 RENDER_CONTROLLER 库）：v6 条目链完整性——
     * 列表端口多头（LIST_MULTI_HEAD）、next 链成环（LIST_CYCLE）、条目不在任何到达
     * rc.root 的链上（ENTRY_ORPHAN，含子图条目）。
     */
    static void checkEntryChains(GraphLibrary library, List<Diagnostic> out) {
        if (library.kind() != GraphKind.CLIENT_ENTITY && library.kind() != GraphKind.RENDER_CONTROLLER) {
            return;
        }
        GraphData main = library.graphs().get(library.main());
        Set<String> claimed = new HashSet<>();
        Set<String> cycleReported = new HashSet<>();
        if (main != null) {
            for (NodeInstance node : main.nodes()) {
                if (!node.type().equals("rc.root")) {
                    continue;
                }
                for (Map.Entry<String, String> port : CHAIN_HEAD_OF_PORT.entrySet()) {
                    List<Wire> direct = main.wires().stream()
                            .filter(w -> w.to().node().equals(node.uid())
                                    && w.to().port().equals(port.getKey()))
                            .toList();
                    if (direct.size() > 1) {
                        out.add(Diagnostic.error(GraphValidator.LIST_MULTI_HEAD,
                                "rc.root 的 " + port.getKey() + " 直连了 " + direct.size()
                                        + " 个条目；v6 起列表是链式：只接链头，后续条目挂到前一条目的 next",
                                node.uid()));
                    }
                    for (Wire headWire : direct) {
                        walkEntryChain(main, headWire.from().node(), claimed, cycleReported, out, true);
                    }
                }
            }
        }
        // 孤儿条目：主图未被链认领 + 子图全部条目（到不了 rc.root）
        for (Map.Entry<String, GraphData> e : library.graphs().entrySet()) {
            boolean isMain = e.getKey().equals(library.main());
            for (NodeInstance node : e.getValue().nodes()) {
                if (!CHAIN_ENTRY_TYPES.contains(node.type())) {
                    continue;
                }
                // 脱链的环：从未被认领的条目起走访（只查环，不认领——不到 rc.root 的链无归属）
                if (!claimed.contains(node.uid())) {
                    walkEntryChain(e.getValue(), node.uid(), claimed, cycleReported, out, false);
                }
                if (!isMain || !claimed.contains(node.uid())) {
                    out.add(Diagnostic.warning(GraphValidator.ENTRY_ORPHAN,
                            node.type() + " 不在任何到达 rc.root 的条目链上"
                                    + (isMain ? "" : "（位于子图 '" + e.getKey() + "'）") + "，不会出现在输出",
                            node.uid()));
                }
            }
        }
    }

    /**
     * 从条目 uid 沿 next 链走访（next 的连线源 = 后继）；环报 LIST_CYCLE。
     * claim=true（rc.root 链头走访）时认领经过的节点；false 仅查环（脱链表走访不认领）。
     */
    private static void walkEntryChain(GraphData graph, String startUid, Set<String> claimed,
                                       Set<String> cycleReported, List<Diagnostic> out, boolean claim) {
        if (claimed.contains(startUid)) {
            return;
        }
        Set<String> path = new LinkedHashSet<>();
        String cur = startUid;
        while (cur != null) {
            if (!path.add(cur)) {
                if (cycleReported.add(cur)) {
                    out.add(Diagnostic.error(GraphValidator.LIST_CYCLE,
                            "条目 next 链成环（经过 " + cur + "）", cur));
                }
                return;
            }
            if (claimed.contains(cur)) {
                break;
            }
            String next = null;
            for (Wire w : graph.wires()) {
                if (w.to().node().equals(cur) && w.to().port().equals("next")) {
                    next = w.from().node();
                    break;
                }
            }
            cur = next;
        }
        if (claim) {
            claimed.addAll(path);
        }
    }
}
