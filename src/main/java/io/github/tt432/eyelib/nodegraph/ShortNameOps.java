package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ref 节点短名相关的图库级改写（规格 nodegraph-shortname-elimination D4）：
 *
 * <ul>
 *   <li>{@link #normalize}：规范化——剥离全部 ref 节点的显式 {@code short_name}
 *       （回到派生），用户主动的「消除短名」动作。适用全闭包已入图的场景；
 *       同 pack 未导入文档若引用旧短名将断（编辑器侧负责提示）。</li>
 *   <li>{@link #backfillIdentifiers}：标识符补全——反编译 RC/AC 产生的裸短名 ref
 *       （无标识符、无预览）按已知短名表回填标识符选项；显式短名保留
 *       （默认「保留原契约」策略，规范化是独立的用户动作）。</li>
 * </ul>
 *
 * <p>两者都是 domain 纯函数：输入库 → 新库 + 变更计数，不改入参。
 */
public final class ShortNameOps {
    private ShortNameOps() {
    }

    /**
     * 协议短名：规范化时保留。{@code default} 是运行时万能回退键（geometry null 回退、
     * texture.material 动态值回退、texture_mesh 回退、外部 vanilla 风格 RC 惯例）；
     * {@code material}（仅 ref.texture）是 Bedrock {@code texture.material} 动态材质纹理协议字。
     * 这些不是可派生的资产引用，而是协议常量。
     */
    static boolean isProtocolShortName(String nodeType, String shortName) {
        return "default".equals(shortName)
                || ("material".equals(shortName) && nodeType.equals("ref.texture"));
    }

    /**
     * 已知短名表：ref 节点类型（ref.geometry/ref.texture/...）→ 短名 → 标识符。
     * 收集侧在 client 层（注册表/已导入实体图库），domain 只消费结构。
     */
    public record KnownTables(Map<String, Map<String, String>> byRefType) {
        public static final KnownTables EMPTY = new KnownTables(Map.of());

        public boolean isEmpty() {
            return byRefType.values().stream().allMatch(Map::isEmpty);
        }

        /** 查表：先精确，后小写（运行时 scope 键 lowercase，大小写不敏感——规格 F1/F9）。 */
        public @org.jspecify.annotations.Nullable String lookup(String refType, String shortName) {
            Map<String, String> table = byRefType.get(refType);
            String hit = table == null ? null : table.get(shortName);
            if (hit == null) {
                table = byRefType.get(refType);
                if (table != null) {
                    hit = table.get(shortName.toLowerCase(java.util.Locale.ROOT));
                }
            }
            return hit;
        }

        public static final class Builder {
            private final Map<String, Map<String, String>> map = new LinkedHashMap<>();

            /** 登记一条短名映射（先登记者胜，与声明表去重同规则）。 */
            public Builder put(String refType, String shortName, String identifier) {
                map.computeIfAbsent(refType, k -> new LinkedHashMap<>())
                        .putIfAbsent(shortName, identifier);
                return this;
            }

            public KnownTables build() {
                return new KnownTables(map);
            }
        }
    }

    public record RewriteResult(GraphLibrary library, int changed) {
    }

    /** 规范化：剥离全部 ref 节点的显式 short_name（键移除，回落类型默认空 = 派生）。
     * 协议短名（{@link #isProtocolShortName}）保留——它们承载运行时回退语义，不是间接噪声。 */
    public static RewriteResult normalize(GraphLibrary library) {
        int[] changed = {0};
        GraphLibrary out = rewriteRefs(library, node -> {
            String explicit = node.options().containsKey(ShortNames.SHORT_NAME_OPTION)
                    ? node.options().get(ShortNames.SHORT_NAME_OPTION).getAsString() : null;
            if (explicit == null || isProtocolShortName(node.type(), explicit)) {
                return node;
            }
            changed[0]++;
            Map<String, JsonElement> options = new LinkedHashMap<>(node.options());
            options.remove(ShortNames.SHORT_NAME_OPTION);
            return copyWithOptions(node, options);
        });
        return new RewriteResult(out, changed[0]);
    }

    /**
     * 标识符补全：ref 节点标识符选项为空/缺失且短名命中已知表 → 回填标识符。
     * ref.animation 额外回落查 ref.ac 表（animations 合并命名空间，D6）。
     * 显式 short_name 一律保留。
     */
    public static RewriteResult backfillIdentifiers(GraphLibrary library, KnownTables known) {
        if (known.isEmpty()) {
            return new RewriteResult(library, 0);
        }
        int[] changed = {0};
        GraphLibrary out = rewriteRefs(library, node -> {
            String valueOption = ShortNames.valueOptionOf(node.type());
            if (valueOption == null) {
                return node;
            }
            String current = node.options().containsKey(valueOption)
                    ? node.options().get(valueOption).getAsString() : "";
            if (!current.isEmpty()) {
                return node;
            }
            String shortName = node.options().containsKey(ShortNames.SHORT_NAME_OPTION)
                    ? node.options().get(ShortNames.SHORT_NAME_OPTION).getAsString() : "";
            if (shortName.isEmpty()) {
                return node;
            }
            String identifier = known.lookup(node.type(), shortName);
            if (identifier == null && node.type().equals("ref.animation")) {
                identifier = known.lookup("ref.ac", shortName);
            }
            if (identifier == null) {
                return node;
            }
            changed[0]++;
            Map<String, JsonElement> options = new LinkedHashMap<>(node.options());
            options.put(valueOption, new JsonPrimitive(identifier));
            return copyWithOptions(node, options);
        });
        return new RewriteResult(out, changed[0]);
    }

    // ---------- 内部 ----------

    private interface NodeRewrite {
        NodeInstance apply(NodeInstance node);
    }

    /** 对库内全部图的 ref 节点应用改写（非 ref 节点原样保留）。 */
    private static GraphLibrary rewriteRefs(GraphLibrary library, NodeRewrite rewrite) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> e : library.graphs().entrySet()) {
            GraphData g = e.getValue();
            List<NodeInstance> nodes = new ArrayList<>(g.nodes().size());
            boolean any = false;
            for (NodeInstance node : g.nodes()) {
                NodeInstance next = ShortNames.valueOptionOf(node.type()) != null
                        ? rewrite.apply(node) : node;
                any |= next != node;
                nodes.add(next);
            }
            graphs.put(e.getKey(), any
                    ? new GraphData(nodes, g.wires(), g.variables(), g.placemats(),
                            g.stickyNotes(), g.graphInterface())
                    : g);
        }
        return new GraphLibrary(library.formatVersion(), library.kind(), library.main(), graphs);
    }

    private static NodeInstance copyWithOptions(NodeInstance node, Map<String, JsonElement> options) {
        return new NodeInstance(node.uid(), node.type(), node.x(), node.y(), options, node.constants());
    }
}
