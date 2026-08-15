package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortDef;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import io.github.tt432.eyelib.nodegraph.Wire;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 导入期初始化赋值折叠（规格 nodegraph-init-default-fold）：「只写不读的常量初始化语句」
 * （{@code variable.x = 常量}，位于 event.initialize 链上，v13 事件模型）折叠为变量声明的默认值，
 * 删除 set_var 与 variable 节点。导入时无条件启用——判据严格到产物语义等价：
 * 原语句经 initialize 链导出，折叠后经默认值导出，同进 scripts.initialize。
 *
 * <p>折叠条件（全部满足，任一不满足即跳过，宁漏勿错）：
 * <ul>
 *   <li>恰好一次写入（variable 节点唯一出边 → set_var.target）且零读取（无其它出边）；</li>
 * <li>写入值为常量：value 端口未连线（行内常量，缺省取端口默认值）或连线自 const.* 节点；</li>
 *   <li>从 set_var 沿 exec_in 反向的路径只经过 exec.set_var/exec.set_temp，
 *       链首挂在 event.initialize（pre_animation/动画链不折——常量虽值等价，
 *       但导出位置变化，严格等价不折）；</li>
 *   <li>变量名未以文本形式出现在任何行内 molang 常量中（variable.x / v.x 两种写法都挡）；</li>
 *   <li>同图无第二个同名 variable 节点（绑定无歧义）；声明已有不同默认值时不折。</li>
 * </ul>
 *
 * <p>等价性论证：折叠把写入从「链中位置」提前到「initialize 最前」（默认值导出为前置）。
 * 链上被跨越的语句若读取该变量必为图内读取（零读取已排除）或行内文本读取
 * （文本引用已排除），外部文件无法在 initialize 执行途中插入读取，故重排不可观测。
 */
final class InitDefaultFolder {
    private InitDefaultFolder() {
    }

    static final String INIT_DEFAULT_FOLD = "INIT_DEFAULT_FOLD";

    /** 每趟折一个候选；上限防意外死循环（正常远早收敛）。 */
    private static final int MAX_PASSES = 256;

    record Result(List<NodeInstance> nodes, List<Wire> wires,
                  List<VariableDecl> foldedDecls, List<Diagnostic> diagnostics) {
    }

    static Result fold(Collection<NodeInstance> startNodes, List<Wire> startWires,
                       List<VariableDecl> existingDecls) {
        List<NodeInstance> nodes = new ArrayList<>(startNodes);
        List<Wire> wires = new ArrayList<>(startWires);
        List<VariableDecl> folded = new ArrayList<>();
        for (int pass = 0; pass < MAX_PASSES; pass++) {
            Candidate candidate = findCandidate(nodes, wires, existingDecls, folded);
            if (candidate == null) {
                break;
            }
            apply(candidate, nodes, wires, folded);
        }
        List<Diagnostic> diagnostics = folded.isEmpty() ? List.of()
                : List.of(Diagnostic.info(INIT_DEFAULT_FOLD,
                        "折叠 " + folded.size() + " 个常量初始化赋值语句为变量默认值"));
        return new Result(List.copyOf(nodes), List.copyOf(wires),
                List.copyOf(folded), diagnostics);
    }

    // ---------- 候选识别 ----------

    private record Candidate(String setUid, String refUid, @Nullable String constUid,
                             String name, JsonElement constant) {
    }

    private static @Nullable Candidate findCandidate(List<NodeInstance> nodes, List<Wire> wires,
                                                     List<VariableDecl> existingDecls,
                                                     List<VariableDecl> folded) {
        Map<String, NodeInstance> byUid = new LinkedHashMap<>();
        Map<String, List<Wire>> fromOut = new LinkedHashMap<>();
        Map<String, Map<String, Wire>> into = new LinkedHashMap<>();
        Map<String, List<Wire>> intoVarIn = new LinkedHashMap<>();
        List<String> constantTexts = new ArrayList<>();
        for (NodeInstance n : nodes) {
            byUid.put(n.uid(), n);
            for (JsonElement value : n.constants().values()) {
                if (value instanceof JsonPrimitive p && p.isString()) {
                    constantTexts.add(p.getAsString());
                }
            }
        }
        for (Wire w : wires) {
            fromOut.computeIfAbsent(w.from().node(), k -> new ArrayList<>()).add(w);
            into.computeIfAbsent(w.to().node(), k -> new LinkedHashMap<>()).put(w.to().port(), w);
            if ("in".equals(w.to().port())) {
                intoVarIn.computeIfAbsent(w.to().node(), k -> new ArrayList<>()).add(w);
            }
        }
        for (NodeInstance v : nodes) {
            if (!"variable".equals(v.type())) {
                continue;
            }
            Candidate c = tryCandidate(v, byUid, fromOut, into, intoVarIn, constantTexts, existingDecls, folded);
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    private static @Nullable Candidate tryCandidate(NodeInstance v,
                                                    Map<String, NodeInstance> byUid,
                                                    Map<String, List<Wire>> fromOut,
                                                    Map<String, Map<String, Wire>> into,
                                                    Map<String, List<Wire>> intoVarIn,
                                                    List<String> constantTexts,
                                                    List<VariableDecl> existingDecls,
                                                    List<VariableDecl> folded) {
        String name = stripRoot(v.optionString("name", ""));
        if (name.isBlank()) {
            return null;
        }
        // 零读（v.out 无出线）+ 单写（v.in 唯一入线且来自 set_var.target）（v9 结构判据）
        if (!fromOut.getOrDefault(v.uid(), List.of()).isEmpty()) {
            return null;
        }
        List<Wire> writes = intoVarIn.getOrDefault(v.uid(), List.of());
        if (writes.size() != 1 || !"target".equals(writes.get(0).from().port())) {
            return null;
        }
        NodeInstance set = byUid.get(writes.get(0).from().node());
        if (set == null || !"exec.set_var".equals(set.type())) {
            return null;
        }
        // 同名第二个 variable 节点 → 绑定歧义
        for (NodeInstance n : byUid.values()) {
            if (!n.uid().equals(v.uid()) && "variable".equals(n.type())
                    && stripRoot(n.optionString("name", "")).equals(name)) {
                return null;
            }
        }
        // 行内 molang 文本引用（图结构不可见）
        if (textuallyReferenced(constantTexts, name)) {
            return null;
        }
        // 常量值：未连线取行内常量（constants 覆盖）或端口默认；连线必须是 const.* 节点
        JsonElement constant;
        String constUid = null;
        Map<String, Wire> setInto = into.getOrDefault(set.uid(), Map.of());
        Wire valueWire = setInto.get("value");
        if (valueWire == null) {
            JsonElement inline = set.constants().get("value");
            constant = inline != null && inline.isJsonPrimitive() ? inline : valueDefaultOf(set);
        } else {
            NodeInstance src = byUid.get(valueWire.from().node());
            if (src == null || !src.type().startsWith("const.")) {
                return null;
            }
            JsonElement value = src.options().get("value");
            if (value == null || !value.isJsonPrimitive()) {
                return null;
            }
            constant = value;
            constUid = src.uid();
        }
        // exec 路径：从 set 沿 exec_in 反向，路径只经 set_var/set_temp，链首挂在 event.initialize
        if (!anchoredAtInitializeEvent(set.uid(), byUid, into)) {
            return null;
        }
        // 声明冲突：已有不同默认值不折（相等视为同一语义，照常折）
        for (VariableDecl d : existingDecls) {
            if (d.name().equals(name) && d.defaultValue().isPresent()
                    && !d.defaultValue().get().equals(constant)) {
                return null;
            }
        }
        for (VariableDecl d : folded) {
            if (d.name().equals(name)) {
                return null; // 已折过同名（防御：单写判据已保证唯一）
            }
        }
        return new Candidate(set.uid(), v.uid(), constUid, name, constant);
    }

    /**
     * set 沿 exec_in 反向（v13 事件模型）：路径节点只允许 set_var/set_temp，
     * 链首的 exec_in 必须接 event.initialize 的 exec_out（pre_animation/动画链不折——
     * 常量虽值等价，但导出位置变化，严格等价不折）。
     */
    private static boolean anchoredAtInitializeEvent(String setUid, Map<String, NodeInstance> byUid,
                                                     Map<String, Map<String, Wire>> into) {
        Set<String> visited = new HashSet<>();
        String cur = setUid;
        while (visited.add(cur)) {
            NodeInstance node = byUid.get(cur);
            if (node == null) {
                return false;
            }
            if (!cur.equals(setUid)
                    && !"exec.set_var".equals(node.type()) && !"exec.set_temp".equals(node.type())) {
                return false;
            }
            Wire back = into.getOrDefault(cur, Map.of()).get("exec_in");
            if (back == null) {
                return false; // 链首悬空：未挂任何时机锚点，折叠会新增从未执行的语句
            }
            NodeInstance prev = byUid.get(back.from().node());
            if (prev == null) {
                return false;
            }
            if ("event.initialize".equals(prev.type())) {
                return "exec_out".equals(back.from().port());
            }
            cur = prev.uid();
        }
        return false;
    }

    // ---------- 应用 ----------

    private static void apply(Candidate c, List<NodeInstance> nodes, List<Wire> wires,
                              List<VariableDecl> folded) {
        List<PortRef> execSources = new ArrayList<>();
        List<PortRef> execTargets = new ArrayList<>();
        for (Wire w : wires) {
            if (w.to().node().equals(c.setUid()) && "exec_in".equals(w.to().port())) {
                execSources.add(w.from());
            }
            if (w.from().node().equals(c.setUid()) && "exec_out".equals(w.from().port())) {
                execTargets.add(w.to());
            }
        }
        wires.removeIf(w -> w.from().node().equals(c.setUid()) || w.to().node().equals(c.setUid())
                || w.from().node().equals(c.refUid()) || w.to().node().equals(c.refUid()));
        for (PortRef source : execSources) {
            for (PortRef target : execTargets) {
                wires.add(new Wire(source, target));
            }
        }
        nodes.removeIf(n -> n.uid().equals(c.setUid()) || n.uid().equals(c.refUid()));
        // 常量源节点变孤儿（无任何出边）则一并删除
        if (c.constUid() != null) {
            String constUid = c.constUid();
            boolean used = wires.stream().anyMatch(w -> w.from().node().equals(constUid));
            if (!used) {
                nodes.removeIf(n -> n.uid().equals(constUid));
            }
        }
        folded.add(new VariableDecl(c.name(), typeOf(c.constant()), Optional.empty(),
                Optional.of(c.constant()), VariableDecl.Scope.VARIABLE));
    }

    private static PortType typeOf(JsonElement constant) {
        // 规则收口到 MolangLiterals.portTypeOf；非基元兜底 STRING（literal() 本就不产非基元）
        PortType type = io.github.tt432.eyelib.nodegraph.MolangLiterals.portTypeOf(constant);
        return type != null ? type : PortType.STRING;
    }

    private static JsonElement valueDefaultOf(NodeInstance set) {
        Optional<NodeType> type = NodeTypes.get(set.type());
        if (type.isPresent()) {
            for (PortDef in : type.get().inputsOf(set, name -> Optional.empty())) {
                if ("value".equals(in.id()) && in.defaultValue().isPresent()) {
                    return in.defaultValue().get();
                }
            }
        }
        return new JsonPrimitive(0);
    }

    /** 变量名以文本形式出现在行内 molang 常量（variable.x / v.x，词边界匹配，宁保守）。 */
    private static boolean textuallyReferenced(List<String> constantTexts, String name) {
        for (String qualified : new String[]{"variable." + name, "v." + name}) {
            Pattern token = Pattern.compile("(?<![\\w.])" + Pattern.quote(qualified) + "(?![\\w])");
            for (String text : constantTexts) {
                if (token.matcher(text).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 剥根前缀（variable./v.）；无前缀原样。 */
    private static String stripRoot(String name) {
        if (name.startsWith("variable.")) {
            return name.substring("variable.".length());
        }
        if (name.startsWith("v.")) {
            return name.substring(2);
        }
        return name;
    }
}
