package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortDef;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.Wire;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 单用 const 常量节点自动内联（规格 nodegraph-import-const-inline）：
 * const.number/int/bool/string 仅有一条出边、且目标是「带默认值的可内联值端口」
 * （INT/FLOAT/BOOL/STRING/ANY，即行内编辑器覆盖的类型）时，常量写入目标节点的
 * 行内常量（constants），删除 const 节点与连线——与端口行内手敲值同一存储路径，
 * 导出产物不变。
 *
 * <p>判据严格：多条出边（共享常量，内联会复制值且失去单一改点）、const.color
 * （COLOR 端口无行内字面值编辑器）、无默认值端口（无行内编辑器，值会隐身）均不动。
 * 字符串值保持 JsonPrimitive 字符串形态——不经过 InlineLiteral.parse 的
 * 智能解析，const.string("5") 不会被退化成 int 5。
 */
final class ConstNodeInliner {
    private ConstNodeInliner() {
    }

    static final String CONST_INLINE = "CONST_INLINE";

    private static final Set<String> CONST_TYPES = Set.of(
            "const.number", "const.int", "const.bool", "const.string");

    private static final Set<PortType> INLINE_CAPABLE = Set.of(
            PortType.INT, PortType.FLOAT, PortType.BOOL, PortType.STRING, PortType.ANY);

    record Result(List<NodeInstance> nodes, List<Wire> wires, List<Diagnostic> diagnostics) {
    }

    static Result inline(Collection<NodeInstance> nodes, List<Wire> wires) {
        Map<String, NodeInstance> byUid = new HashMap<>();
        Map<String, Integer> outDegree = new HashMap<>();
        for (NodeInstance n : nodes) {
            byUid.put(n.uid(), n);
        }
        for (Wire w : wires) {
            outDegree.merge(w.from().node(), 1, Integer::sum);
        }

        Set<String> removedConst = new HashSet<>();
        // 目标 uid → (端口 id → 内联值)
        Map<String, Map<String, JsonElement>> patches = new HashMap<>();
        List<Wire> kept = new ArrayList<>();
        for (Wire w : wires) {
            NodeInstance from = byUid.get(w.from().node());
            if (from == null || !CONST_TYPES.contains(from.type())
                    || outDegree.getOrDefault(from.uid(), 0) != 1) {
                kept.add(w);
                continue;
            }
            NodeInstance to = byUid.get(w.to().node());
            if (to == null || !inlineCapable(to, w.to().port())) {
                kept.add(w);
                continue;
            }
            JsonElement value = from.options().get("value");
            if (value == null) {
                kept.add(w);
                continue;
            }
            patches.computeIfAbsent(to.uid(), k -> new HashMap<>()).put(w.to().port(), value);
            removedConst.add(from.uid());
        }
        if (removedConst.isEmpty()) {
            return new Result(List.copyOf(nodes), wires, List.of());
        }

        List<NodeInstance> outNodes = new ArrayList<>();
        for (NodeInstance n : nodes) {
            if (removedConst.contains(n.uid())) {
                continue;
            }
            Map<String, JsonElement> patch = patches.get(n.uid());
            if (patch == null) {
                outNodes.add(n);
                continue;
            }
            Map<String, JsonElement> merged = new java.util.LinkedHashMap<>(n.constants());
            merged.putAll(patch);
            outNodes.add(new NodeInstance(n.uid(), n.type(), n.x(), n.y(), n.options(), merged));
        }
        return new Result(List.copyOf(outNodes), List.copyOf(kept),
                List.of(Diagnostic.info(CONST_INLINE,
                        "已内联 " + removedConst.size() + " 个单用 const 常量节点到端口行内值")));
    }

    /** 端口可内联：带默认值且类型有行内编辑器（同 EvmInlinePortFields 覆盖范围）。 */
    private static boolean inlineCapable(NodeInstance target, String portId) {
        Optional<NodeType> type = NodeTypes.get(target.type());
        if (type.isEmpty()) {
            return false;
        }
        for (PortDef def : type.get().inputsOf(target, name -> Optional.empty())) {
            if (def.id().equals(portId)) {
                return def.defaultValue().isPresent() && INLINE_CAPABLE.contains(def.type());
            }
        }
        return false;
    }
}
