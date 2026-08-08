package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.StickyNote;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * 导入图构建器（包内共享）：节点/连线/便签/诊断累积、反编译片段合并（uid 加唯一前缀防冲突）、
 * 值槽/EXEC 槽接线、黑板变量收集，最终经 {@link GraphLayout} 布局产出 {@link ImportResult}。
 */
final class ImportGraphBuilder {
    private final Map<String, NodeInstance> nodes = new LinkedHashMap<>();
    private final List<Wire> wires = new ArrayList<>();
    private final List<StickyNote> stickies = new ArrayList<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private int uidSeq;
    private int fragmentSeq;
    private int stickySeq;
    /** 导入期变量内联开关（规格 nodegraph-import-inline-variables；默认关，UI 复选框 opt-in）。 */
    private boolean inlineVariables;

    void inlineVariables(boolean inlineVariables) {
        this.inlineVariables = inlineVariables;
    }

    // ---------- 选项/节点构造 ----------

    /** 选项表构造（String/Number/Boolean/JsonElement → JsonPrimitive/原样），同 AssemblyTestSupport 风格。 */
    static Map<String, JsonElement> opts(Object... kv) {
        Map<String, JsonElement> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            JsonElement e = v instanceof String s ? new JsonPrimitive(s)
                    : v instanceof Number n ? new JsonPrimitive(n)
                    : v instanceof Boolean bl ? new JsonPrimitive(bl)
                    : (JsonElement) v;
            map.put((String) kv[i], e);
        }
        return map;
    }

    /** 根锚点节点：uid 固定 "root"（与 ng_smoke.json 约定一致）。 */
    void addRoot(String type, Map<String, JsonElement> options) {
        nodes.put("root", new NodeInstance("root", type, 0, 0, options, Map.of()));
    }

    /** 普通节点：uid = 语义前缀 + 递增序号（确定性）。 */
    String addNode(String prefix, String type, Map<String, JsonElement> options) {
        String uid = prefix + (uidSeq++);
        nodes.put(uid, new NodeInstance(uid, type, 0, 0, options, Map.of()));
        return uid;
    }

    /** 未连线输入端口的内联值（constants；优先级高于端口默认，codegen 经 literal 发射）。 */
    void putConstant(String nodeUid, String portId, JsonElement value) {
        NodeInstance n = nodes.get(nodeUid);
        if (n == null) {
            throw new IllegalStateException("node '" + nodeUid + "' not found");
        }
        Map<String, JsonElement> constants = new LinkedHashMap<>(n.constants());
        constants.put(portId, value);
        nodes.put(nodeUid, new NodeInstance(n.uid(), n.type(), n.x(), n.y(), n.options(), constants));
    }

    void wire(String fromNode, String fromPort, String toNode, String toPort) {
        wires.add(new Wire(new PortRef(fromNode, fromPort), new PortRef(toNode, toPort)));
    }

    void sticky(String text) {
        stickies.add(new StickyNote("note" + (stickySeq++), text, 0, 0, 200, 100, "#FFFF88"));
    }

    void warn(String code, String message) {
        diagnostics.add(Diagnostic.warning(code, message));
    }

    void info(String code, String message) {
        diagnostics.add(Diagnostic.info(code, message));
    }

    /** 节点类型（无此节点 → null）。 */
    @Nullable String typeOf(String uid) {
        NodeInstance n = nodes.get(uid);
        return n == null ? null : n.type();
    }

    /** 是否已存在指定类型、指定 short_name 选项值的 ref 节点（凾底接线判重用）。 */
    boolean hasRefWithShortName(String refType, String shortName) {
        return nodes.values().stream().anyMatch(n -> n.type().equals(refType)
                && n.options().get("short_name") instanceof JsonPrimitive p
                && p.isString() && p.getAsString().equals(shortName));
    }

    /** 是否存在指定类型与短名、且能沿连线到达任一 RC 锚点（rc.root / ref.rc）的 ref。 */
    boolean refReachesAnchor(String refType, String shortName) {
        Map<String, List<String>> forward = new LinkedHashMap<>();
        for (Wire wire : wires) {
            forward.computeIfAbsent(wire.from().node(), k -> new ArrayList<>()).add(wire.to().node());
        }
        for (NodeInstance n : nodes.values()) {
            if (!n.type().equals(refType)
                    || !(n.options().get("short_name") instanceof JsonPrimitive p)
                    || !p.isString() || !p.getAsString().equals(shortName)) {
                continue;
            }
            if (reachesAnchor(n.uid(), forward, new LinkedHashMap<>(), new java.util.HashSet<>())) {
                return true;
            }
        }
        return false;
    }

    private boolean reachesAnchor(String uid, Map<String, List<String>> forward,
                                  Map<String, Boolean> memo, java.util.Set<String> visiting) {
        NodeInstance node = nodes.get(uid);
        if (node != null && (node.type().equals("rc.root") || node.type().equals("ref.rc"))) {
            return true;
        }
        Boolean cached = memo.get(uid);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(uid)) {
            return false;
        }
        boolean hit = false;
        for (String next : forward.getOrDefault(uid, List.of())) {
            if (reachesAnchor(next, forward, memo, visiting)) {
                hit = true;
                break;
            }
        }
        visiting.remove(uid);
        memo.put(uid, hit);
        return hit;
    }

    void error(String code, String message) {
        diagnostics.add(Diagnostic.error(code, message));
    }

    // ---------- 片段合并与槽接线 ----------

    /** 合并反编译片段：全部 uid（节点/连线端点/便签/诊断关联节点）加唯一前缀，返回前缀。 */
    private String mergeFragment(List<NodeInstance> fNodes, List<Wire> fWires,
                                 List<StickyNote> fStickies, List<Diagnostic> fDiagnostics) {
        String prefix = "f" + (fragmentSeq++) + "_";
        for (NodeInstance n : fNodes) {
            nodes.put(prefix + n.uid(),
                    new NodeInstance(prefix + n.uid(), n.type(), 0, 0, n.options(), n.constants()));
        }
        for (Wire w : fWires) {
            wires.add(new Wire(
                    new PortRef(prefix + w.from().node(), w.from().port()),
                    new PortRef(prefix + w.to().node(), w.to().port())));
        }
        for (StickyNote s : fStickies) {
            stickies.add(new StickyNote(prefix + s.uid(), s.text(), 0, 0,
                    s.width(), s.height(), s.color()));
        }
        for (Diagnostic d : fDiagnostics) {
            diagnostics.add(new Diagnostic(d.severity(), d.code(), d.message(),
                    d.nodeUid().map(u -> prefix + u)));
        }
        return prefix;
    }

    /** molang 表达式 → 连线到消费值槽。空白源 = 无内容，跳过。 */
    void wireExpression(String source, String consumerUid, String consumerPort) {
        if (source.isBlank()) {
            return;
        }
        MolangDecompiler.ExprFragment frag = MolangDecompiler.decompileExpression(source);
        String prefix = mergeFragment(frag.nodes(), frag.wires(), frag.stickyNotes(), frag.diagnostics());
        frag.output().ifPresent(out -> wire(prefix + out.node(), out.port(), consumerUid, consumerPort));
    }

    /**
     * molang 语句序列列表（Bedrock 允许 string 或 string[]，调用方拆好）→ exec 链接入消费 EXEC 槽。
     * 多段顺序拼接：前段链尾 exec_out → 后段链首 exec_in；最终链尾 → 消费槽（反向汇入槽模型）。
     */
    void wireStatements(List<String> sources, String consumerUid, String consumerPort) {
        PortRef prevTail = null;
        for (String source : sources) {
            if (source.isBlank()) {
                continue;
            }
            MolangDecompiler.ExecFragment frag = MolangDecompiler.decompileStatements(source);
            String prefix = mergeFragment(frag.nodes(), frag.wires(), frag.stickyNotes(), frag.diagnostics());
            if (frag.chain().isEmpty()) {
                continue;
            }
            if (prevTail != null) {
                wire(prevTail.node(), prevTail.port(), prefix + frag.chain().get(0), "exec_in");
            }
            List<String> chain = frag.chain();
            prevTail = new PortRef(prefix + chain.get(chain.size() - 1), "exec_out");
        }
        if (prevTail != null) {
            wire(prevTail.node(), prevTail.port(), consumerUid, consumerPort);
        }
    }

    // ---------- 产出 ----------

    ImportResult build(GraphKind kind) {
        Collection<NodeInstance> buildNodes = nodes.values();
        List<Wire> buildWires = wires;
        if (inlineVariables) {
            // 内联必须在布局前：删除的节点不占位，布局直接按内联后拓扑排布
            VariableInliner.Result inlined = VariableInliner.inline(buildNodes, buildWires);
            buildNodes = inlined.nodes();
            buildWires = inlined.wires();
            diagnostics.addAll(inlined.diagnostics());
        }
        // 初始化折叠无条件启用（规格 nodegraph-init-default-fold）：判据严格到产物等价
        InitDefaultFolder.Result folded = InitDefaultFolder.fold(buildNodes, buildWires, List.of());
        buildNodes = folded.nodes();
        buildWires = folded.wires();
        diagnostics.addAll(folded.diagnostics());
        // 单用 const 内联无条件启用（规格 nodegraph-import-const-inline）
        ConstNodeInliner.Result constInlined = ConstNodeInliner.inline(buildNodes, buildWires);
        buildNodes = constInlined.nodes();
        buildWires = constInlined.wires();
        diagnostics.addAll(constInlined.diagnostics());
        List<NodeInstance> laidOut = GraphLayout.layout(List.copyOf(buildNodes), buildWires);
        List<StickyNote> placed = GraphLayout.placeStickyNotes(stickies, laidOut);
        GraphData data = new GraphData(laidOut, List.copyOf(buildWires),
                mergeDecls(collectVariables(laidOut), folded.foldedDecls()),
                List.of(), placed, Optional.empty());
        return new ImportResult(
                new GraphLibrary(GraphLibrary.CURRENT_FORMAT_VERSION, kind, "root", Map.of("root", data)),
                diagnostics);
    }

    /** 折叠产出的声明与节点收集合并（折叠声明带默认值，优先保留）。 */
    private static List<VariableDecl> mergeDecls(List<VariableDecl> collected,
                                                 List<VariableDecl> foldedDecls) {
        if (foldedDecls.isEmpty()) {
            return collected;
        }
        Set<String> foldedNames = new TreeSet<>();
        for (VariableDecl d : foldedDecls) {
            foldedNames.add(d.name());
        }
        List<VariableDecl> out = new ArrayList<>(
                collected.stream().filter(d -> !foldedNames.contains(d.name())).toList());
        out.addAll(foldedDecls);
        out.sort((a, b) -> a.name().compareTo(b.name()));
        return List.copyOf(out);
    }

    /** 黑板变量声明：收集 variable 节点引用的变量名（不带根，去重排序）。 */
    private static List<VariableDecl> collectVariables(List<NodeInstance> nodes) {
        Set<String> names = new TreeSet<>();
        for (NodeInstance n : nodes) {
            if ("variable".equals(n.type())) {
                addVarName(names, n.options().get("name"));
            }
        }
        return names.stream().map(name -> VariableDecl.of(name, PortType.ANY)).toList();
    }

    private static void addVarName(Set<String> out, @Nullable JsonElement name) {
        if (name instanceof JsonPrimitive p && p.isString()) {
            String s = p.getAsString();
            // 兼容带根旧写法（variable.foo）；variable 节点 name 规范形态是不带根
            out.add(s.startsWith("variable.") ? s.substring("variable.".length()) : s);
        }
    }
}
