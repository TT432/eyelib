package io.github.tt432.eyelib.nodegraph.assembly;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.Wire;
import io.github.tt432.eyelib.nodegraph.codegen.CodegenResult;
import io.github.tt432.eyelib.nodegraph.codegen.ColorCode;
import io.github.tt432.eyelib.nodegraph.codegen.ColorCodegenResult;
import io.github.tt432.eyelib.nodegraph.codegen.MolangGenerator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * 组装器共享工具：组装上下文（codegen 入口 + 诊断汇总）、SLOT 条目收集、
 * 连线/内容判定、选项读取。
 */
final class AssemblySupport {
    private AssemblySupport() {
    }

    // ---------- 组装器自身诊断码（codegen 诊断经 CodegenResult 透传，不在此列） ----------

    /** 主图缺少（或不止一个）根锚点节点。 */
    static final String MISSING_ROOT = "MISSING_ROOT";
    /** 装配条目的引用槽未连线（animate.entry.ref / rc.condition_entry.rc）。 */
    static final String MISSING_ENTRY_REF = "MISSING_ENTRY_REF";
    /** 装配条目的引用槽连到了种类非法的节点。 */
    static final String INVALID_ENTRY_REF = "INVALID_ENTRY_REF";
    /** entity.root 声明端口的连线源节点类型与该端口声明类别不匹配。 */
    static final String INVALID_DECLARATION_REF = "INVALID_DECLARATION_REF";
    /** rc.root 的 arrays 选项不是合法 JSON 对象。 */
    static final String INVALID_ARRAYS = "INVALID_ARRAYS";
    /** ac.state 重名。 */
    static final String DUPLICATE_STATE = "DUPLICATE_STATE";
    /** initial_state / transition.target 不在 states 键集中。 */
    static final String UNKNOWN_STATE = "UNKNOWN_STATE";

    /** 单次组装的上下文：库 + 主图 + codegen 入口 + 诊断汇总。 */
    static final class Ctx {
        final GraphLibrary library;
        final GraphData main;
        final MolangGenerator generator;
        final List<Diagnostic> diagnostics = new ArrayList<>();

        Ctx(GraphLibrary library, GraphData main) {
            this.library = library;
            this.main = main;
            this.generator = new MolangGenerator(library);
        }

        /** 表达式槽生成（值槽 → ExprSet 字符串）；诊断透传。 */
        String emitExpression(String nodeUid, String portId) {
            CodegenResult r = generator.emitExpressionFor(library.main(), nodeUid, portId);
            diagnostics.addAll(r.diagnostics());
            return r.code();
        }

        /** 执行时机源生成（EXEC OUT 端口 → 语句序列，v13 事件模型）；诊断透传。 */
        String emitStatementsFrom(String nodeUid, String portId) {
            CodegenResult r = generator.emitStatementListFromFor(library.main(), nodeUid, portId);
            diagnostics.addAll(r.diagnostics());
            return r.code();
        }

        /** 颜色槽生成（COLOR 槽 → 四通道）；null = 未连线（调用方省略字段）。诊断透传。 */
        @Nullable ColorCode emitColor(String nodeUid, String portId) {
            ColorCodegenResult r = generator.emitColorFor(library.main(), nodeUid, portId);
            diagnostics.addAll(r.diagnostics());
            return r.code();
        }

        void error(String code, String message, String nodeUid) {
            diagnostics.add(Diagnostic.error(code, message, nodeUid));
        }
    }

    /** 主图根锚点；恰好 1 个才返回（验证器已保证，此处为防御性检查）。 */
    static Optional<NodeInstance> findRoot(Ctx ctx, String rootType) {
        List<NodeInstance> roots = ctx.main.nodes().stream().filter(n -> n.type().equals(rootType)).toList();
        if (roots.size() != 1) {
            ctx.diagnostics.add(Diagnostic.error(MISSING_ROOT,
                    "主图必须恰好 1 个 " + rootType + "，实际 " + roots.size() + "（应先经 GraphValidator 验证）"));
            return Optional.empty();
        }
        return Optional.of(roots.get(0));
    }

    /** 收集连入某输入端口的源节点（按 uid 去重，uid 字典序稳定排序）。 */
    static List<NodeInstance> wiredSources(GraphData graph, String nodeUid, String portId) {
        Set<String> uids = new LinkedHashSet<>();
        for (Wire wire : graph.wires()) {
            if (wire.to().node().equals(nodeUid) && wire.to().port().equals(portId)) {
                uids.add(wire.from().node());
            }
        }
        List<NodeInstance> entries = new ArrayList<>();
        for (String uid : uids) {
            graph.findNode(uid).ifPresent(entries::add);
        }
        entries.sort(Comparator.comparing(NodeInstance::uid));
        return entries;
    }

    /** 指向某输入端口的线（多条时取 from 字典序最小者，与 codegen 同规则）。 */
    static Optional<Wire> wireInto(GraphData graph, String nodeUid, String portId) {
        return graph.wires().stream()
                .filter(w -> w.to().node().equals(nodeUid) && w.to().port().equals(portId))
                .min(Comparator.comparing((Wire w) -> w.from().node()).thenComparing(w -> w.from().port()));
    }

    /**
     * 有序条目链收集（v6，规格 §2.2）：列表端口的直连源为链头（多头按 uid 序兜底），
     * 沿 `next` 链走访产出有序条目；visited 集防环截断。
     */
    static List<NodeInstance> chainSources(GraphData graph, String nodeUid, String portId) {
        List<NodeInstance> out = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (NodeInstance head : wiredSources(graph, nodeUid, portId)) {
            NodeInstance cur = head;
            while (visited.add(cur.uid())) {
                out.add(cur);
                NodeInstance next = wireInto(graph, cur.uid(), "next")
                        .flatMap(w -> graph.findNode(w.from().node()))
                        .orElse(null);
                if (next == null) {
                    break;
                }
                cur = next;
            }
        }
        return out;
    }

    static boolean hasWire(GraphData graph, String nodeUid, String portId) {
        return graph.wires().stream()
                .anyMatch(w -> w.to().node().equals(nodeUid) && w.to().port().equals(portId));
    }

    /** 某输出端口是否有出线（时机源端口判定用）。 */
    static boolean hasWireOut(GraphData graph, String nodeUid, String portId) {
        return graph.wires().stream()
                .anyMatch(w -> w.from().node().equals(nodeUid) && w.from().port().equals(portId));
    }

    /** 值槽「有内容」判定：有连线，或 constants 含该端口内联值（内联值由 codegen 处理）。 */
    static boolean hasContent(GraphData graph, NodeInstance node, String portId) {
        return node.constants().containsKey(portId) || hasWire(graph, node.uid(), portId);
    }

    /** 解析装配条目的引用槽源节点；未连线/悬空时报告 MISSING_ENTRY_REF 并返回 null。 */
    static @Nullable NodeInstance resolveEntryRef(Ctx ctx, NodeInstance entry, String portId) {
        Optional<Wire> wire = wireInto(ctx.main, entry.uid(), portId);
        if (wire.isEmpty()) {
            ctx.error(MISSING_ENTRY_REF,
                    "装配条目 " + entry.uid() + " 的 " + portId + " 槽未连接引用节点", entry.uid());
            return null;
        }
        Optional<NodeInstance> source = ctx.main.findNode(wire.get().from().node());
        if (source.isEmpty()) {
            ctx.error(MISSING_ENTRY_REF,
                    "装配条目 " + entry.uid() + " 的 " + portId + " 槽连线源节点不存在", entry.uid());
            return null;
        }
        return source.get();
    }

    /** 取 string 选项（实例值优先，缺省回落类型默认）。 */
    static String optionString(NodeInstance node, NodeType type, String id) {
        return node.option(id, type).map(JsonElement::getAsString).orElse("");
    }

    /** 取原始 JSON 选项（数值/布尔选项直接写 JSON 原始类型，不走 molang）。 */
    static JsonElement optionValue(NodeInstance node, NodeType type, String id) {
        return node.option(id, type).orElse(new JsonPrimitive(false));
    }
}
