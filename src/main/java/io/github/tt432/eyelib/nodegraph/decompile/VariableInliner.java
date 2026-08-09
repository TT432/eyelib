package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortDef;
import io.github.tt432.eyelib.nodegraph.PortDirection;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.Wire;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 导入期变量内联（规格 nodegraph-import-inline-variables）：把「单写单读」的变量取值
 * 直接接到唯一读取点，删除 set/get 节点。仅 opt-in（导入对话框复选框）启用。
 *
 * <p>可内联条件（全部满足，任一不满足即跳过，宁漏勿错）：
 * <ul>
 *   <li>恰好一次写入（同图同名单 setter）且恰好一次读取（variable 节点 = 非 target 的
 *       唯一出边；temp = 全图唯一 temp.get 的唯一出边）；</li>
 *   <li>读取点的 exec 上下文（值链下游首个 exec 节点，唯一）在 exec 流上可从 set 到达
 *       ——保证「先写后读」，不重排求值顺序；</li>
 *   <li>set 与读取上下文同处循环体内或同处循环体外（跨 loop/for_each body 边界不内联）；</li>
 *   <li>变量名未以文本形式出现在任何行内 molang 常量中（文本引用图结构不可见，
 *       variable.x / v.x / temp.x / t.x 四种写法都挡）；</li>
 *   <li>variable 名在同图无第二个 variable 节点（绑定无歧义）。</li>
 * </ul>
 *
 * <p>值子图在本节点系统的值侧纯表达式（副作用只走 exec 链），单读场景下移动求值位置
 * 不改变结果。variable.* 的写是实体可观测副作用，内联即移除该副作用——由复选框
 * 显式授权。每趟只处理一个候选并重算资格（候选间可能嵌套：set a = f(temp.b)），
 * 不动点收敛（每趟净删 2 节点）。
 */
final class VariableInliner {
    private VariableInliner() {
    }

    static final String INLINED_VARIABLE = "INLINED_VARIABLE";

    /** 每趟删一个候选；上限防意外死循环（正常远早收敛）。 */
    private static final int MAX_PASSES = 256;

    private static final NodeType.SubgraphResolver NO_SUBGRAPH = name -> Optional.empty();

    record Result(List<NodeInstance> nodes, List<Wire> wires, List<Diagnostic> diagnostics) {
    }

    static Result inline(Collection<NodeInstance> startNodes, List<Wire> startWires) {
        List<NodeInstance> nodes = new ArrayList<>(startNodes);
        List<Wire> wires = new ArrayList<>(startWires);
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (int pass = 0; pass < MAX_PASSES; pass++) {
            Candidate candidate = findCandidate(nodes, wires);
            if (candidate == null) {
                break;
            }
            apply(candidate, nodes, wires, diagnostics);
        }
        return new Result(List.copyOf(nodes), List.copyOf(wires), List.copyOf(diagnostics));
    }

    // ---------- 候选识别 ----------

    private record Candidate(String setUid, String refUid, String consumerUid, String consumerPort,
                             @Nullable PortRef valueSource, JsonElement valueDefault, String displayName) {
    }

    private static @Nullable Candidate findCandidate(List<NodeInstance> nodes, List<Wire> wires) {
        Index idx = new Index(nodes, wires);
        for (NodeInstance node : nodes) {
            Candidate c = switch (node.type()) {
                case "variable" -> variableCandidate(node, idx);
                case "exec.set_temp" -> tempCandidate(node, idx);
                default -> null;
            };
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    /** variable 形态：V + 唯一 set_var（V.out→S.target）+ 唯一读（V.out→非 target 端口）。 */
    private static @Nullable Candidate variableCandidate(NodeInstance v, Index idx) {
        String name = stripRoot(v.optionString("name", ""), "variable");
        if (name.isBlank()) {
            return null;
        }
        String setUid = null;
        String consumerUid = null;
        String consumerPort = null;
        // v9 左读右写：写 = set_var.target 输出 → v.in；读 = v.out 全部出线
        for (Wire w : idx.intoVarIn.getOrDefault(v.uid(), List.of())) {
            NodeInstance set = idx.byUid.get(w.from().node());
            if (set == null || !set.type().equals("exec.set_var") || !w.from().port().equals("target")
                    || setUid != null) {
                return null; // 写入者不是 set_var，或多次写入
            }
            setUid = w.from().node();
        }
        for (Wire w : idx.fromOut.getOrDefault(v.uid(), List.of())) {
            if (consumerUid != null) {
                return null; // 多次读取
            }
            consumerUid = w.to().node();
            consumerPort = w.to().port();
        }
        if (setUid == null || consumerUid == null || consumerPort == null) {
            return null;
        }
        // 同名第二个 variable 节点 → 绑定歧义
        for (NodeInstance n : idx.nodes) {
            if (!n.uid().equals(v.uid()) && n.type().equals("variable")
                    && stripRoot(n.optionString("name", ""), "variable").equals(name)) {
                return null;
            }
        }
        if (idx.textuallyReferenced("variable." + name, "v." + name)) {
            return null;
        }
        return finishCandidate(setUid, v.uid(), consumerUid, consumerPort, "variable." + name, idx);
    }

    /** temp 形态：唯一同名 set_temp S + 全图唯一同名 temp.get G + G 唯一读。 */
    private static @Nullable Candidate tempCandidate(NodeInstance set, Index idx) {
        String name = stripRoot(set.optionString("name", ""), "temp");
        if (name.isBlank()) {
            return null;
        }
        NodeInstance getter = null;
        for (NodeInstance n : idx.nodes) {
            if (n.type().equals("exec.set_temp") && !n.uid().equals(set.uid())
                    && stripRoot(n.optionString("name", ""), "temp").equals(name)) {
                return null; // 多次写入
            }
            if (n.type().equals("temp.get")
                    && stripRoot(n.optionString("name", ""), "temp").equals(name)) {
                if (getter != null) {
                    return null; // 多个 get 节点
                }
                getter = n;
            }
        }
        if (getter == null) {
            return null;
        }
        List<Wire> reads = idx.fromOut.getOrDefault(getter.uid(), List.of());
        if (reads.size() != 1) {
            return null;
        }
        if (idx.textuallyReferenced("temp." + name, "t." + name)) {
            return null;
        }
        Wire read = reads.get(0);
        return finishCandidate(set.uid(), getter.uid(), read.to().node(), read.to().port(),
                "temp." + name, idx);
    }

    /** 公共收尾：exec 上下文 + 先写后读 + 循环边界；取值源/默认值。 */
    private static @Nullable Candidate finishCandidate(String setUid, String refUid,
                                                       String consumerUid, String consumerPort,
                                                       String displayName, Index idx) {
        String context = idx.execContextOf(consumerUid);
        if (context == null || !idx.execReachable(setUid, context)) {
            return null;
        }
        if (idx.bodySet.contains(setUid) != idx.bodySet.contains(context)) {
            return null;
        }
        Wire valueWire = idx.wireInto(setUid, "value");
        PortRef valueSource = valueWire != null ? valueWire.from() : null;
        NodeInstance setNode = idx.byUid.get(setUid);
        // 未连线值：行内常量（constants 覆盖）优先，缺省回落端口默认
        JsonElement inlineConstant = setNode != null ? setNode.constants().get("value") : null;
        JsonElement valueDefault = inlineConstant != null && inlineConstant.isJsonPrimitive()
                ? inlineConstant : valueDefaultOf(setNode);
        return new Candidate(setUid, refUid, consumerUid, consumerPort,
                valueSource, valueDefault, displayName);
    }

    private static JsonElement valueDefaultOf(@Nullable NodeInstance set) {
        if (set != null) {
            Optional<NodeType> type = NodeTypes.get(set.type());
            if (type.isPresent()) {
                for (PortDef in : type.get().inputsOf(set, NO_SUBGRAPH)) {
                    if (in.id().equals("value") && in.defaultValue().isPresent()) {
                        return in.defaultValue().get();
                    }
                }
            }
        }
        return new JsonPrimitive(0);
    }

    // ---------- 应用 ----------

    private static void apply(Candidate c, List<NodeInstance> nodes, List<Wire> wires,
                              List<Diagnostic> diagnostics) {
        // exec 旁路：S 的上游 exec 源 × S 的下游 exec 目标
        List<PortRef> execSources = new ArrayList<>();
        List<PortRef> execTargets = new ArrayList<>();
        for (Wire w : wires) {
            if (w.to().node().equals(c.setUid()) && w.to().port().equals("exec_in")) {
                execSources.add(w.from());
            }
            if (w.from().node().equals(c.setUid()) && w.from().port().equals("exec_out")) {
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

        if (c.valueSource() != null) {
            wires.add(new Wire(c.valueSource(), new PortRef(c.consumerUid(), c.consumerPort())));
            setConstant(nodes, c.consumerUid(), c.consumerPort(), null);
        } else {
            setConstant(nodes, c.consumerUid(), c.consumerPort(), c.valueDefault());
        }
        nodes.removeIf(n -> n.uid().equals(c.setUid()) || n.uid().equals(c.refUid()));
        diagnostics.add(Diagnostic.info(INLINED_VARIABLE,
                "内联 " + c.displayName() + "：值直连读取点，set/get 节点已移除"));
    }

    /** 写/清消费端口行内常量（value=null 表示删除该键）。 */
    private static void setConstant(List<NodeInstance> nodes, String uid, String port,
                                    @Nullable JsonElement value) {
        for (int i = 0; i < nodes.size(); i++) {
            NodeInstance n = nodes.get(i);
            if (n.uid().equals(uid)) {
                Map<String, JsonElement> constants = new LinkedHashMap<>(n.constants());
                if (value == null) {
                    constants.remove(port);
                } else {
                    constants.put(port, value);
                }
                nodes.set(i, new NodeInstance(n.uid(), n.type(), n.x(), n.y(),
                        n.options(), constants));
                return;
            }
        }
    }

    // ---------- 索引与图分析 ----------

    private static final class Index {
        final List<NodeInstance> nodes;
        final Map<String, NodeInstance> byUid = new LinkedHashMap<>();
        /** 源节点 → 出边列表。 */
        final Map<String, List<Wire>> fromOut = new LinkedHashMap<>();
        /** 目标 (node,port) → 入边。 */
        final Map<String, Map<String, Wire>> into = new LinkedHashMap<>();
        /** variable 节点 in 端口的全部入边（v9 写入通道；multi 故需独立索引）。 */
        final Map<String, List<Wire>> intoVarIn = new LinkedHashMap<>();
        /** exec 前向邻接（from.port == exec_out）。 */
        final Map<String, List<String>> execFwd = new LinkedHashMap<>();
        /** 循环体成员（可达某 loop/for_each 的 body 输入的全部 exec 节点）。 */
        final Set<String> bodySet = new HashSet<>();
        /** 全图行内 molang 常量文本（文本引用扫描用）。 */
        final List<String> constantTexts = new ArrayList<>();

        Index(List<NodeInstance> nodes, List<Wire> wires) {
            this.nodes = nodes;
            for (NodeInstance n : nodes) {
                byUid.put(n.uid(), n);
                for (JsonElement value : n.constants().values()) {
                    if (value instanceof JsonPrimitive p && p.isString()) {
                        constantTexts.add(p.getAsString());
                    }
                }
            }
            Map<String, List<String>> execBack = new LinkedHashMap<>();
            for (Wire w : wires) {
                fromOut.computeIfAbsent(w.from().node(), k -> new ArrayList<>()).add(w);
                into.computeIfAbsent(w.to().node(), k -> new LinkedHashMap<>())
                        .put(w.to().port(), w);
                if (w.to().port().equals("in")) {
                    intoVarIn.computeIfAbsent(w.to().node(), k -> new ArrayList<>()).add(w);
                }
                if (w.from().port().equals("exec_out")) {
                    execFwd.computeIfAbsent(w.from().node(), k -> new ArrayList<>())
                            .add(w.to().node());
                    execBack.computeIfAbsent(w.to().node(), k -> new ArrayList<>())
                            .add(w.from().node());
                }
            }
            // body 输入的入边源沿 exec 反向走访 → 循环体成员
            for (Wire w : wires) {
                if (!w.to().port().equals("body")) {
                    continue;
                }
                Deque<String> stack = new ArrayDeque<>();
                stack.push(w.from().node());
                while (!stack.isEmpty()) {
                    String cur = stack.pop();
                    if (!bodySet.add(cur)) {
                        continue;
                    }
                    stack.addAll(execBack.getOrDefault(cur, List.of()));
                }
            }
        }

        @Nullable Wire wireInto(String uid, String port) {
            Map<String, Wire> byPort = into.get(uid);
            return byPort == null ? null : byPort.get(port);
        }

        /** 读取点的 exec 上下文：本身是 exec 节点即自身；否则沿值边下游找唯一 exec 节点。 */
        @Nullable String execContextOf(String uid) {
            if (isExecNode(uid)) {
                return uid;
            }
            Set<String> contexts = new HashSet<>();
            Deque<String> stack = new ArrayDeque<>();
            Set<String> visited = new HashSet<>();
            stack.push(uid);
            while (!stack.isEmpty()) {
                String cur = stack.pop();
                if (!visited.add(cur)) {
                    continue;
                }
                if (isExecNode(cur)) {
                    contexts.add(cur);
                    continue;
                }
                for (Wire w : fromOut.getOrDefault(cur, List.of())) {
                    stack.push(w.to().node());
                }
            }
            return contexts.size() == 1 ? contexts.iterator().next() : null;
        }

        boolean isExecNode(String uid) {
            NodeInstance node = byUid.get(uid);
            if (node == null) {
                return false;
            }
            Optional<NodeType> type = NodeTypes.get(node.type());
            return type.isPresent() && type.get().inputsOf(node, NO_SUBGRAPH).stream()
                    .anyMatch(p -> p.direction() == PortDirection.IN && p.type() == PortType.EXEC);
        }

        /** exec 流可达性（先写后读判据）。 */
        boolean execReachable(String from, String to) {
            Deque<String> stack = new ArrayDeque<>();
            Set<String> visited = new HashSet<>();
            stack.push(from);
            while (!stack.isEmpty()) {
                String cur = stack.pop();
                if (cur.equals(to)) {
                    return true;
                }
                if (!visited.add(cur)) {
                    continue;
                }
                stack.addAll(execFwd.getOrDefault(cur, List.of()));
            }
            return false;
        }

        /** 变量名以文本形式出现在行内 molang 常量（词边界匹配，宁保守）。 */
        boolean textuallyReferenced(String... qualifiedNames) {
            for (String qualified : qualifiedNames) {
                Pattern token = Pattern.compile("(?<![\\w.])" + Pattern.quote(qualified) + "(?![\\w])");
                for (String text : constantTexts) {
                    if (token.matcher(text).find()) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /** 剥根前缀（variable./v. 或 temp./t.）；无前缀原样。 */
    private static String stripRoot(String name, String root) {
        if (name.startsWith(root + ".")) {
            return name.substring(root.length() + 1);
        }
        String shortAlias = root.equals("variable") ? "v." : "t.";
        if (name.startsWith(shortAlias)) {
            return name.substring(shortAlias.length());
        }
        return name;
    }
}
