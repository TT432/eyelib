package io.github.tt432.eyelib.nodegraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * geo/tex/mat 声明表派生（规格 nodegraph-inline-render-controller §3.1）：
 * 组装器、验证器（REF_CONFLICT / REF_NOT_CONNECTED 范围）、client KnownRefTables 的单一口径。
 *
 * <p><b>可达性模型</b>：ref.{geometry,texture,material} 在主图中存在到任一
 * <b>RC 锚点</b>（rc.root / ref.rc）的连线路径 → 入声明表（「ref 接 RC = 声明+引用」）。
 * v6 起 rc.root 无 decl_* 端口——其引用集由 geometry/textures/materials 三值端口决定
 * （规格 nodegraph-ordered-entries-and-rc-reference-set §3.1），覆盖字段端口直连、条目 value、
 * 以及嵌套在表达式树内部的情形（悦灵 RC 的 {@code query.x ? geometry.a : geometry.b} 变体选择）；
 * ref.rc 保留 decl_* 声明端口（外部 RC 无值端口）。协议短名 ref（default / texture.material）
 * 在图中出现即入表（carve-out，规格 §3.2）。其余无路径 → 不声明（验证器 REF_NOT_CONNECTED）。
 *
 * <p>同有效短名 putIfAbsent 保留先者；同短名不同标识符的冲突由验证器 REF_CONFLICT 报告。
 * 声明端口（decl_geometries 等）的类别不匹配源由 {@link #invalidDeclarationSources}
 * 列出（组装器报 INVALID_DECLARATION_REF）——但可达性模型下该 ref 仍入其本类别表
 * （尽力产出：错误由诊断承担，表保持语义完整）。
 */
public final class DeclarationTables {
    private DeclarationTables() {
    }

    /** 派生三表：geometry / textures / materials（短名 → 资产标识符，插入序 = 主图节点序）。 */
    public record Tables(Map<String, String> geometry, Map<String, String> textures,
                         Map<String, String> materials) {
    }

    /** 类别不匹配的声明连线（anchor uid, source node, port id）。 */
    public record InvalidRef(String anchorUid, NodeInstance source, String port) {
    }

    /** 主图 RC 锚点（rc.root / ref.rc），uid 字典序。 */
    public static List<NodeInstance> rcAnchors(GraphData main) {
        return main.nodes().stream()
                .filter(n -> n.type().equals("rc.root") || n.type().equals("ref.rc"))
                .sorted(Comparator.comparing(NodeInstance::uid))
                .toList();
    }

    /** 派生三表。 */
    public static Tables collect(GraphData main) {
        Map<String, List<NodeInstance>> refs = collectRefs(main);
        Map<String, String> geometry = new LinkedHashMap<>();
        Map<String, String> textures = new LinkedHashMap<>();
        Map<String, String> materials = new LinkedHashMap<>();
        // collectRefs 固定初始化三类键（结构性不变量）
        Objects.requireNonNull(refs.get("geometry")).forEach(n -> putShortName(n, geometry));
        Objects.requireNonNull(refs.get("textures")).forEach(n -> putShortName(n, textures));
        Objects.requireNonNull(refs.get("materials")).forEach(n -> putShortName(n, materials));
        return new Tables(geometry, textures, materials);
    }

    /** 声明集合的 per-ref 视图（类别 → ref 节点，uid 序）：验证器冲突检测用。
     * v6：协议短名 ref（default / texture.material）在图中出现即入集，无需接线（规格 §3.2）。 */
    public static Map<String, List<NodeInstance>> collectRefs(GraphData main) {
        Map<String, List<NodeInstance>> out = new LinkedHashMap<>();
        out.put("geometry", new ArrayList<>());
        out.put("textures", new ArrayList<>());
        out.put("materials", new ArrayList<>());
        Set<String> anchors = new HashSet<>();
        for (NodeInstance anchor : rcAnchors(main)) {
            anchors.add(anchor.uid());
        }
        // 前向邻接：生产者 → 消费者
        Map<String, List<String>> forward = new HashMap<>();
        for (Wire wire : main.wires()) {
            forward.computeIfAbsent(wire.from().node(), k -> new ArrayList<>()).add(wire.to().node());
        }
        Map<String, Boolean> memo = new HashMap<>();
        for (NodeInstance node : main.nodes()) {
            String category = switch (node.type()) {
                case "ref.geometry" -> "geometry";
                case "ref.texture" -> "textures";
                case "ref.material" -> "materials";
                default -> null;
            };
            if (category == null) {
                continue;
            }
            boolean declared = reachesAnchor(node.uid(), anchors, forward, memo, new HashSet<>());
            if (!declared) {
                // 协议短名 carve-out：运行时回退/模型契约常量，不挂字段表达式也必须入表
                String shortName = ShortNames.effective(node, NodeTypes.require(node.type()));
                declared = ShortNameOps.isProtocolShortName(node.type(), shortName);
            }
            if (declared) {
                Objects.requireNonNull(out.get(category)).add(node);
            }
        }
        return out;
    }

    /** 类别不匹配的声明端口连线源（类型系统外的手工构造 JSON 防御）。 */
    public static List<InvalidRef> invalidDeclarationSources(GraphData main) {
        List<InvalidRef> out = new ArrayList<>();
        for (NodeInstance anchor : rcAnchors(main)) {
            for (Map.Entry<String, String> e : NodeTypes.RC_DECLARATION_PORTS.entrySet()) {
                String refType = e.getKey();
                String port = e.getValue();
                for (Wire wire : main.wires()) {
                    if (!wire.to().node().equals(anchor.uid()) || !wire.to().port().equals(port)) {
                        continue;
                    }
                    main.findNode(wire.from().node()).ifPresent(source -> {
                        if (!source.type().equals(refType)) {
                            out.add(new InvalidRef(anchor.uid(), source, port));
                        }
                    });
                }
            }
        }
        return out;
    }

    // ---------- 内部 ----------

    /** uid 出发能否沿连线到达任一 RC 锚点（带记忆化与环防御）。 */
    private static boolean reachesAnchor(String uid, Set<String> anchors,
                                         Map<String, List<String>> forward,
                                         Map<String, Boolean> memo, Set<String> visiting) {
        if (anchors.contains(uid)) {
            return true;
        }
        Boolean cached = memo.get(uid);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(uid)) {
            return false; // 环：本轮不再深入（环由 GraphValidator.CYCLE 报告）
        }
        boolean hit = false;
        for (String next : forward.getOrDefault(uid, List.of())) {
            if (reachesAnchor(next, anchors, forward, memo, visiting)) {
                hit = true;
                break;
            }
        }
        visiting.remove(uid);
        memo.put(uid, hit);
        return hit;
    }

    private static void putShortName(NodeInstance ref, Map<String, String> table) {
        NodeType type = NodeTypes.require(ref.type());
        String valueOption = ShortNames.valueOptionOf(ref.type());
        table.putIfAbsent(ShortNames.effective(ref, type),
                valueOption == null ? "" : ref.option(valueOption, type)
                        .map(e -> e.getAsString()).orElse(""));
    }
}
