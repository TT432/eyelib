package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortDef;
import io.github.tt432.eyelib.nodegraph.PortDirection;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
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
 * 导入期初始化赋值折叠（规格 nodegraph-init-default-fold）：「常量初始化语句」
 * （{@code variable.x = 常量}，位于 event.initialize 链上，v13 事件模型）折叠为变量声明的默认值，
 * 删除 set_var 与写入侧 variable 节点。导入时无条件启用——判据严格到产物语义等价：
 * 原语句经 initialize 链导出，折叠后经默认值导出，同进 scripts.initialize。
 *
 * <p>折叠条件（全部满足，任一不满足即跳过，宁漏勿错）：
 * <ul>
 *   <li>写入唯一：本 variable 节点唯一入边 → set_var.target，且同图同名 variable
 *       节点均无写入通道（in 端口入边）；</li>
 *   <li>读取不早于写入（读取允许存在）：每个读取的 exec 上下文（值链下游 exec 节点）
 *       要么在 exec 流上可从 set 到达（读在写后），要么执行链不锚在 event.initialize
 *       （pre_animation/纯值语境/悬空链均在 initialize 完成后才求值，恒观察到默认值）；
 *       下游分叉到多个 exec 语境（时机歧义）拒绝；</li>
 *   <li>写入值为常量：value 端口未连线（行内常量，缺省取端口默认值）或连线自 const.* 节点；</li>
 *   <li>从 set_var 沿 exec_in 反向的路径只经过 exec.set_var/exec.set_temp，
 *       链首挂在 event.initialize（pre_animation/动画链不折——常量虽值等价，
 *       但导出位置变化，严格等价不折）；</li>
 *   <li>变量名未以文本形式出现在任何行内 molang 常量中（variable.x / v.x 两种写法都挡）；</li>
 *   <li>声明已有不同默认值时不折。</li>
 * </ul>
 *
 * <p>等价性论证：折叠把写入从「链中位置」提前到「initialize 最前」（默认值导出为前置）。
 * 早于原写入位置的读取只可能存在于同一 initialize 链上游——「读取不早于写入」判据
 * 已排除；其余读取（其它事件链/纯值语境）在原语义下也在 initialize 完成后求值，
 * 此时变量已被写入常量，与默认值等价。文本引用图结构不可见，一律拒绝。
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
        // exec 邻接（from.port == exec_out）：前向用于「读在写后」可达性，反向用于锚点追溯
        Map<String, List<String>> execFwd = new LinkedHashMap<>();
        Map<String, List<String>> execBack = new LinkedHashMap<>();
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
            if ("exec_out".equals(w.from().port())) {
                execFwd.computeIfAbsent(w.from().node(), k -> new ArrayList<>()).add(w.to().node());
                execBack.computeIfAbsent(w.to().node(), k -> new ArrayList<>()).add(w.from().node());
            }
        }
        for (NodeInstance v : nodes) {
            if (!"variable".equals(v.type())) {
                continue;
            }
            Candidate c = tryCandidate(v, byUid, fromOut, into, intoVarIn, execFwd, execBack,
                    constantTexts, existingDecls, folded);
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
                                                    Map<String, List<String>> execFwd,
                                                    Map<String, List<String>> execBack,
                                                    List<String> constantTexts,
                                                    List<VariableDecl> existingDecls,
                                                    List<VariableDecl> folded) {
        String name = stripRoot(v.optionString("name", ""));
        if (name.isBlank()) {
            return null;
        }
        // 单写（v.in 唯一入线且来自 set_var.target）（v9 结构判据）
        List<Wire> writes = intoVarIn.getOrDefault(v.uid(), List.of());
        if (writes.size() != 1 || !"target".equals(writes.get(0).from().port())) {
            return null;
        }
        NodeInstance set = byUid.get(writes.get(0).from().node());
        if (set == null || !"exec.set_var".equals(set.type())) {
            return null;
        }
        // 同名其它 variable 节点：有第二个写入通道 → 不折；读边逐个查时机（读不早于写，
        // 读边本身保留——声明带上默认值后读取照常读到同一常量）
        for (NodeInstance n : byUid.values()) {
            if (n.uid().equals(v.uid()) || !"variable".equals(n.type())
                    || !stripRoot(n.optionString("name", "")).equals(name)) {
                continue;
            }
            if (!intoVarIn.getOrDefault(n.uid(), List.of()).isEmpty()) {
                return null;
            }
            for (Wire read : fromOut.getOrDefault(n.uid(), List.of())) {
                if (!readCannotPrecedeInit(read.to().node(), set.uid(), byUid, fromOut,
                        execFwd, execBack)) {
                    return null;
                }
            }
        }
        // 本节点的读边同样要求读不早于写
        for (Wire read : fromOut.getOrDefault(v.uid(), List.of())) {
            if (!readCannotPrecedeInit(read.to().node(), set.uid(), byUid, fromOut,
                    execFwd, execBack)) {
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

    // ---------- 读取时机判据 ----------

    private static final NodeType.SubgraphResolver NO_SUBGRAPH = name -> Optional.empty();

    /**
     * 读取不可能早于 initialize 链上的写入：
     * <ul>
     *   <li>读取无 exec 语境（纯值使用：RC 表达式/animate 条件/根字段）→ 安全，
     *       这些语境在 initialize 完成后才求值；下游分叉到多个 exec 语境 → 时机歧义，拒绝；</li>
     *   <li>唯一 exec 语境可从 set 沿 exec 流到达 → 读在写后，安全；</li>
     *   <li>否则仅当该语境的执行链不锚在 event.initialize 才安全（其它事件/悬空链
     *       不可能在 initialize 完成前执行）；锚在 initialize 且不可达 → 可能先读后写，拒绝。</li>
     * </ul>
     */
    private static boolean readCannotPrecedeInit(String consumerUid, String setUid,
                                                 Map<String, NodeInstance> byUid,
                                                 Map<String, List<Wire>> fromOut,
                                                 Map<String, List<String>> execFwd,
                                                 Map<String, List<String>> execBack) {
        Set<String> contexts = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        stack.push(consumerUid);
        while (!stack.isEmpty()) {
            String cur = stack.pop();
            if (!visited.add(cur)) {
                continue;
            }
            if (isExecNode(cur, byUid)) {
                contexts.add(cur);
                continue;
            }
            for (Wire w : fromOut.getOrDefault(cur, List.of())) {
                stack.push(w.to().node());
            }
        }
        if (contexts.size() > 1) {
            return false;
        }
        if (contexts.isEmpty()) {
            return true;
        }
        String context = contexts.iterator().next();
        if (execReachable(setUid, context, execFwd)) {
            return true;
        }
        return !hasEventAnchor(context, "event.initialize", byUid, execBack);
    }

    /** 节点是否 exec 节点（有 EXEC 输入端口）。 */
    private static boolean isExecNode(String uid, Map<String, NodeInstance> byUid) {
        NodeInstance node = byUid.get(uid);
        if (node == null) {
            return false;
        }
        Optional<NodeType> type = NodeTypes.get(node.type());
        return type.isPresent() && type.get().inputsOf(node, NO_SUBGRAPH).stream()
                .anyMatch(p -> p.direction() == PortDirection.IN && p.type() == PortType.EXEC);
    }

    /** exec 流前向可达性（先写后读判据）。 */
    private static boolean execReachable(String from, String to, Map<String, List<String>> execFwd) {
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

    /** 沿 exec 流反向追溯，执行链是否锚在指定事件节点（任一前驱链锚中即算）。 */
    private static boolean hasEventAnchor(String uid, String eventType,
                                          Map<String, NodeInstance> byUid,
                                          Map<String, List<String>> execBack) {
        Deque<String> stack = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        stack.push(uid);
        while (!stack.isEmpty()) {
            String cur = stack.pop();
            if (!visited.add(cur)) {
                continue;
            }
            NodeInstance node = byUid.get(cur);
            if (node == null) {
                continue;
            }
            if (eventType.equals(node.type())) {
                return true;
            }
            stack.addAll(execBack.getOrDefault(cur, List.of()));
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
        wires.removeIf(w -> w.from().node().equals(c.setUid()) || w.to().node().equals(c.setUid()));
        for (PortRef source : execSources) {
            for (PortRef target : execTargets) {
                wires.add(new Wire(source, target));
            }
        }
        nodes.removeIf(n -> n.uid().equals(c.setUid()));
        // 写入侧 variable 节点：自带读边时保留为纯读节点（声明默认值承接其值），无读边才删除
        boolean refHasReads = wires.stream().anyMatch(w -> w.from().node().equals(c.refUid()));
        if (!refHasReads) {
            nodes.removeIf(n -> n.uid().equals(c.refUid()));
        }
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
