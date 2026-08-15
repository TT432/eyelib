package io.github.tt432.eyelib.nodegraph.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphInterface;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.Wire;
import io.github.tt432.eyelib.nodegraph.PortRef;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * {@link MolangGenerator} 单测：黄金字符串覆盖 §2.4 发射表全类节点、共享子表达式提取、
 * exec 链（loop/for_each 嵌套）、子图内联展开（名称隔离/递归检测）、未连接输入诊断、常量转义。
 *
 * <p>表达式槽统一用 entity.root 的 scale/scale_x/scale_y/scale_z 作汇；执行链由 event.* 时机源
 * 节点的 exec_out 出发（v13，规格 nodegraph-event-nodes）。
 * 图直接用 record 构造，不走 JSON。
 */
class MolangGeneratorTest {

    // ---------- 构造辅助 ----------

    private static NodeInstance node(String uid, String type) {
        return NodeInstance.of(uid, type, 0, 0);
    }

    private static NodeInstance node(String uid, String type, Map<String, JsonElement> options) {
        return new NodeInstance(uid, type, 0, 0, options, Map.of());
    }

    private static NodeInstance node(String uid, String type, Map<String, JsonElement> options,
                                     Map<String, JsonElement> constants) {
        return new NodeInstance(uid, type, 0, 0, options, constants);
    }

    private static Map<String, JsonElement> opts(Object... kv) {
        Map<String, JsonElement> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            JsonElement e = v instanceof String s ? new JsonPrimitive(s)
                    : v instanceof Number n ? new JsonPrimitive(n)
                    : v instanceof Boolean b ? new JsonPrimitive(b)
                    : (JsonElement) v;
            map.put((String) kv[i], e);
        }
        return map;
    }

    private static Wire wire(String fromNode, String fromPort, String toNode, String toPort) {
        return new Wire(new PortRef(fromNode, fromPort), new PortRef(toNode, toPort));
    }

    private static GraphData graph(List<NodeInstance> nodes, List<Wire> wires) {
        return new GraphData(nodes, wires, List.of(), List.of(), List.of(), Optional.empty());
    }

    private static GraphData subgraph(GraphInterface iface, List<NodeInstance> nodes, List<Wire> wires) {
        return new GraphData(nodes, wires, List.of(), List.of(), List.of(), Optional.of(iface));
    }

    private static GraphLibrary library(GraphData main) {
        return new GraphLibrary(1, GraphKind.CLIENT_ENTITY, "root", Map.of("root", main));
    }

    private static GraphLibrary library(GraphData main, Map<String, GraphData> subgraphs) {
        Map<String, GraphData> graphs = new LinkedHashMap<>(subgraphs);
        graphs.put("root", main);
        return new GraphLibrary(1, GraphKind.CLIENT_ENTITY, "root", graphs);
    }

    private static NodeInstance root() {
        return node("root", "entity.root");
    }

    private static String expr(GraphLibrary lib, String port) {
        CodegenResult r = new MolangGenerator(lib).emitExpressionFor("root", "root", port);
        assertFalse(r.hasErrors(), () -> "unexpected errors: " + r.diagnostics());
        return r.code();
    }

    /** 从 event.* 时机源节点的 exec_out 正向发射语句序列。 */
    private static String exec(GraphLibrary lib, String eventUid) {
        CodegenResult r = new MolangGenerator(lib).emitStatementListFromFor("root", eventUid, "exec_out");
        assertFalse(r.hasErrors(), () -> "unexpected errors: " + r.diagnostics());
        return r.code();
    }

    // ---------- 常量 ----------

    @Test
    void constNumberFormatsIntegersAndDecimals() {
        GraphData main = graph(
                List.of(root(),
                        node("i", "const.number", opts("value", 1.0)),
                        node("d", "const.number", opts("value", 1.5))),
                List.of(wire("i", "out", "root", "scale"),
                        wire("d", "out", "root", "scale_x")));
        GraphLibrary lib = library(main);
        assertEquals("1", expr(lib, "scale"));
        assertEquals("1.5", expr(lib, "scale_x"));
    }

    @Test
    void constIntEmitsIntegerLiteral() {
        GraphData main = graph(
                List.of(root(), node("i", "const.int", opts("value", 42))),
                List.of(wire("i", "out", "root", "scale")));
        assertEquals("42", expr(library(main), "scale"));
    }

    @Test
    void constBoolEmitsOneAndZero() {
        GraphData main = graph(
                List.of(root(),
                        node("t", "const.bool", opts("value", true)),
                        node("f", "const.bool", opts("value", false))),
                List.of(wire("t", "out", "root", "scale"),
                        wire("f", "out", "root", "scale_x")));
        GraphLibrary lib = library(main);
        assertEquals("1", expr(lib, "scale"));
        assertEquals("0", expr(lib, "scale_x"));
    }

    @Test
    void constStringEscapesQuoteAndBackslash() {
        GraphData main = graph(
                List.of(root(), node("s", "const.string", opts("value", "it's a \\ test"))),
                List.of(wire("s", "out", "root", "scale")));
        assertEquals("'it\\'s a \\\\ test'", expr(library(main), "scale"));
    }

    @Test
    void contextGetPrefixesName() {
        GraphData main = graph(
                List.of(root(),
                        node("c", "context.get", opts("name", "item_slot")),
                        node("d", "context.get", opts("name", "context.other"))),
                List.of(wire("c", "out", "root", "scale"),
                        wire("d", "out", "root", "scale_x")));
        GraphLibrary lib = library(main);
        assertEquals("context.item_slot", expr(lib, "scale"));
        assertEquals("context.other", expr(lib, "scale_x"));
    }

    @Test
    void nodeOutputEmissionForBadge() {
        // 画布徽标路径：直接发射生产者输出（无需下游输入槽）。
        GraphData main = graph(
                List.of(root(),
                        node("i", "const.number", opts("value", 1.0)),
                        node("add", "op.add"),
                        node("j", "const.number", opts("value", 2))),
                List.of(wire("i", "out", "add", "a"),
                        wire("j", "out", "add", "b"),
                        wire("add", "out", "root", "scale")));
        GraphLibrary lib = library(main);
        CodegenResult leaf = new MolangGenerator(lib).emitNodeOutput("root", "i", "out");
        assertFalse(leaf.hasErrors(), () -> "unexpected errors: " + leaf.diagnostics());
        assertEquals("1", leaf.code());
        CodegenResult sum = new MolangGenerator(lib).emitNodeOutput("root", "add", "out");
        assertFalse(sum.hasErrors(), () -> "unexpected errors: " + sum.diagnostics());
        assertEquals("(1 + 2)", sum.code());
    }

    @Test
    void nodeOutputEmissionRejectsValueInput() {
        GraphData main = graph(List.of(root()), List.of());
        CodegenResult r = new MolangGenerator(library(main)).emitNodeOutput("root", "root", "scale");
        assertTrue(r.hasErrors());
    }

    // ---------- 变量 / 查询 / 数学 ----------

    @Test
    void varAndTempGetPrefixNames() {
        GraphData main = graph(
                List.of(root(),
                        node("a", "variable", opts("name", "foo")),
                        node("b", "variable", opts("name", "bar")),
                        node("c", "temp.get", opts("name", "t1"))),
                List.of(wire("a", "out", "root", "scale"),
                        wire("b", "out", "root", "scale_x"),
                        wire("c", "out", "root", "scale_y")));
        GraphLibrary lib = library(main);
        assertEquals("variable.foo", expr(lib, "scale"));
        assertEquals("variable.bar", expr(lib, "scale_x"));
        assertEquals("temp.t1", expr(lib, "scale_y"));
    }

    @Test
    void queryCallPropertyAccessAndCallForm() {
        // v11：query.anim_time 不在签名表（零参内建不入表）→ 无端口无列表条目 → 属性访问形；
        // 连线进参数的场景改用定长签名函数（query.camera_distance_range_lerp 双参 → arg1/arg2 端口）
        GraphData main = graph(
                List.of(root(),
                        node("q0", "query.call", opts("function", "query.anim_time")),
                        node("q1", "query.call", opts("function", "query.camera_distance_range_lerp")),
                        node("v", "variable", opts("name", "x")),
                        node("c", "const.number", opts("value", 1.5))),
                List.of(wire("q0", "out", "root", "scale"),
                        wire("q1", "out", "root", "scale_x"),
                        wire("v", "out", "q1", "arg1"),
                        wire("c", "out", "q1", "arg2")));
        GraphLibrary lib = library(main);
        assertEquals("query.anim_time", expr(lib, "scale"));
        assertEquals("query.camera_distance_range_lerp(variable.x, 1.5)", expr(lib, "scale_x"));
    }

    @Test
    void mathCallEmitsFunctionForm() {
        GraphData main = graph(
                List.of(root(),
                        node("m", "math.call", opts("function", "math.sin", "arg_count", 1)),
                        node("q", "query.call", opts("function", "query.anim_time", "arg_count", 0))),
                List.of(wire("m", "out", "root", "scale"),
                        wire("q", "out", "m", "arg1")));
        assertEquals("math.sin(query.anim_time)", expr(library(main), "scale"));
    }

    // ---------- 运算（全括号化） ----------

    @Test
    void binaryNestedIsFullyParenthesized() {
        // a + b * c（query 引用作叶子）→ (query.a + (query.b * query.c))
        GraphData main = graph(
                List.of(root(),
                        node("add", "op.add"),
                        node("mul", "op.multiply"),
                        node("a", "query.call", opts("function", "query.a", "arg_count", 0)),
                        node("b", "query.call", opts("function", "query.b", "arg_count", 0)),
                        node("c", "query.call", opts("function", "query.c", "arg_count", 0))),
                List.of(wire("add", "out", "root", "scale"),
                        wire("a", "out", "add", "a"),
                        wire("mul", "out", "add", "b"),
                        wire("b", "out", "mul", "a"),
                        wire("c", "out", "mul", "b")));
        assertEquals("(query.a + (query.b * query.c))", expr(library(main), "scale"));
    }

    @Test
    void unaryTernaryNullCoalesceAreFullyParenthesized() {
        GraphData main = graph(
                List.of(root(),
                        node("neg", "op.negate"),
                        node("v", "variable", opts("name", "a")),
                        node("ter", "op.ternary"),
                        node("cond", "query.call", opts("function", "query.c", "arg_count", 0)),
                        node("nc", "op.null_coalesce"),
                        node("v2", "variable", opts("name", "b"))),
                List.of(wire("neg", "out", "root", "scale"),
                        wire("v", "out", "neg", "a"),
                        wire("ter", "out", "root", "scale_x"),
                        wire("cond", "out", "ter", "cond"),
                        wire("v", "out", "ter", "a"),
                        wire("v2", "out", "ter", "b"),
                        wire("nc", "out", "root", "scale_y"),
                        wire("v", "out", "nc", "a"),
                        wire("v2", "out", "nc", "b")));
        GraphLibrary lib = library(main);
        assertEquals("(-variable.a)", expr(lib, "scale"));
        assertEquals("(query.c ? variable.a : variable.b)", expr(lib, "scale_x"));
        assertEquals("(variable.a ?? variable.b)", expr(lib, "scale_y"));
    }

    // ---------- 共享子表达式提取（§2.4-3） ----------

    @Test
    void sharedSubexpressionIsExtractedToTemp() {
        // mul = add * add，add = query.x + 1（出度 2 且非平凡）
        GraphData main = graph(
                List.of(root(),
                        node("add", "op.add"),
                        node("x", "query.call", opts("function", "query.x", "arg_count", 0)),
                        node("one", "const.number", opts("value", 1)),
                        node("mul", "op.multiply")),
                List.of(wire("mul", "out", "root", "scale"),
                        wire("x", "out", "add", "a"),
                        wire("one", "out", "add", "b"),
                        wire("add", "out", "mul", "a"),
                        wire("add", "out", "mul", "b")));
        assertEquals("temp.g0 = (query.x + 1); (temp.g0 * temp.g0)", expr(library(main), "scale"));
    }

    @Test
    void trivialNodesAreNotExtractedEvenWhenShared() {
        // variable 节点出度 2 仍是平凡（单变量引用），文本内联
        GraphData main = graph(
                List.of(root(),
                        node("v", "variable", opts("name", "a")),
                        node("add", "op.add")),
                List.of(wire("add", "out", "root", "scale"),
                        wire("v", "out", "add", "a"),
                        wire("v", "out", "add", "b")));
        assertEquals("(variable.a + variable.a)", expr(library(main), "scale"));
    }

    // ---------- 资源引用 ----------

    @Test
    void referenceNodesEmitShortNames() {
        GraphData main = graph(
                List.of(root(),
                        node("g", "ref.geometry", opts("short_name", "default", "identifier", "geometry.x.y")),
                        node("t", "ref.texture", opts("short_name", "skin", "path", "textures/entity/x")),
                        node("m", "ref.material", opts("short_name", "mat", "material", "entity_alphatest"))),
                List.of(wire("g", "ref", "root", "scale"),
                        wire("t", "ref", "root", "scale_x"),
                        wire("m", "ref", "root", "scale_y")));
        GraphLibrary lib = library(main);
        assertEquals("geometry.default", expr(lib, "scale"));
        assertEquals("texture.skin", expr(lib, "scale_x"));
        assertEquals("material.mat", expr(lib, "scale_y"));
    }

    @Test
    void referenceNodesDeriveShortNamesWhenUnset() {
        // short_name 空 = 派生（D1）：identity+sanitize
        GraphData main = graph(
                List.of(root(),
                        node("g", "ref.geometry", opts("identifier", "geometry.x.y")),
                        node("t", "ref.texture", opts("path", "textures/entity/x")),
                        node("m", "ref.material", opts("material", "Entity_Alphatest"))),
                List.of(wire("g", "ref", "root", "scale"),
                        wire("t", "ref", "root", "scale_x"),
                        wire("m", "ref", "root", "scale_y")));
        GraphLibrary lib = library(main);
        assertEquals("geometry.geometry.x.y", expr(lib, "scale"));
        assertEquals("texture.textures.entity.x", expr(lib, "scale_x"));
        assertEquals("material.entity_alphatest", expr(lib, "scale_y"));
    }

    // ---------- 未连接输入 ----------

    @Test
    void unconnectedInputFallsBackToConstantThenDefault() {
        // add.a 未连线但 constants 内联 2.5；add.b 未连线 → 端口默认 0
        GraphData main = graph(
                List.of(root(),
                        node("add", "op.add", Map.of(), opts("a", 2.5))),
                List.of(wire("add", "out", "root", "scale")));
        assertEquals("(2.5 + 0)", expr(library(main), "scale"));
    }

    @Test
    void unconnectedInputWithoutDefaultIsError() {
        // v11：未知函数已无端口可断连；改用定长签名函数验证真实行为——
        // math.sin 的 arg1 端口（inLabeled，无默认值）未连线 → UNCONNECTED_INPUT 错误 + 0 占位
        GraphData main = graph(
                List.of(root(),
                        node("q", "math.call", opts("function", "math.sin"))),
                List.of(wire("q", "out", "root", "scale")));
        CodegenResult r = new MolangGenerator(library(main)).emitExpressionFor("root", "root", "scale");
        assertEquals("math.sin(0)", r.code());
        assertTrue(r.hasErrors());
        assertTrue(r.diagnostics().stream().anyMatch(d -> d.code().equals("UNCONNECTED_INPUT")));
    }

    // ---------- 执行链 ----------

    @Test
    void initializeChainEmitsSetVarStatements() {
        // set1: variable.a = 1（variable 节点连线 target）→ set2: temp.t = (variable.a * 2)
        GraphData main = graph(
                List.of(root(),
                        node("ev", "event.initialize"),
                        node("set1", "exec.set_var"),
                        node("ta", "variable", opts("name", "a")),
                        node("c1", "const.number", opts("value", 1)),
                        node("set2", "exec.set_temp", opts("name", "t")),
                        node("mul", "op.multiply"),
                        node("va", "variable", opts("name", "a")),
                        node("c2", "const.number", opts("value", 2))),
                List.of(wire("ev", "exec_out", "set1", "exec_in"),
                        wire("set1", "exec_out", "set2", "exec_in"),
                        wire("set1", "target", "ta", "in"),
                        wire("c1", "out", "set1", "value"),
                        wire("mul", "out", "set2", "value"),
                        wire("va", "out", "mul", "a"),
                        wire("c2", "out", "mul", "b")));
        assertEquals("variable.a = 1; temp.t = (variable.a * 2)", exec(library(main), "ev"));
    }

    @Test
    void emptyExecSlotEmitsZero() {
        // event 节点存在但 exec_out 悬空 → 空链 → "0"
        GraphData main = graph(List.of(root(), node("ev", "event.initialize")), List.of());
        assertEquals("0", exec(library(main), "ev"));
    }

    @Test
    void loopAndForEachNest() {
        // initialize: loop(3, { for_each(temp.item, query.my_array, { temp.sum = (temp.sum + temp.item) }) })
        GraphData main = graph(
                List.of(root(),
                        node("ev", "event.initialize"),
                        node("loop", "exec.loop"),
                        node("c3", "const.number", opts("value", 3)),
                        node("fe", "exec.for_each", opts("var_name", "temp.item")),
                        node("arr", "query.call", opts("function", "query.my_array", "arg_count", 0)),
                        node("set", "exec.set_temp", opts("name", "sum")),
                        node("add", "op.add"),
                        node("sum", "temp.get", opts("name", "temp.sum")),
                        node("item", "temp.get", opts("name", "temp.item"))),
                List.of(wire("ev", "exec_out", "loop", "exec_in"),
                        wire("c3", "out", "loop", "count"),
                        wire("fe", "exec_out", "loop", "body"),
                        wire("arr", "out", "fe", "array"),
                        wire("set", "exec_out", "fe", "body"),
                        wire("add", "out", "set", "value"),
                        wire("sum", "out", "add", "a"),
                        wire("item", "out", "add", "b")));
        assertEquals("loop(3, { for_each(temp.item, query.my_array, { temp.sum = (temp.sum + temp.item) }) })",
                exec(library(main), "ev"));
    }

    @Test
    void execCallBreakContinueReturn() {
        // 注：br/cont 的 exec_out 仅用于验证发射规则本身（body 槽、链尾悬空均合法）。
        GraphData main = graph(
                List.of(root(),
                        node("ev_init", "event.initialize"),
                        node("ev_pre", "event.pre_animation"),
                        node("ev_parent", "event.parent_setup"),
                        // v11：连线进 call 参数需定长签名函数（math.pow 双参 → arg1/arg2 端口）
                        node("call", "exec.call", opts("function", "math.pow")),
                        node("c1", "const.number", opts("value", 1)),
                        node("v", "variable", opts("name", "x")),
                        node("loop", "exec.loop", Map.of(), opts("count", 10)),
                        node("br", "exec.break"),
                        node("cont", "exec.continue")),
                List.of(wire("ev_init", "exec_out", "call", "exec_in"),
                        wire("c1", "out", "call", "arg1"),
                        wire("v", "out", "call", "arg2"),
                        wire("ev_pre", "exec_out", "loop", "exec_in"),
                        wire("br", "exec_out", "loop", "body"),
                        wire("ev_parent", "exec_out", "cont", "exec_in")));
        GraphLibrary lib = library(main);
        assertEquals("math.pow(1, variable.x)", exec(lib, "ev_init"));
        assertEquals("loop(10, { break })", exec(lib, "ev_pre"));
        assertEquals("continue", exec(lib, "ev_parent"));
    }

    @Test
    void execReturnEmitsReturnStatement() {
        GraphData main = graph(
                List.of(root(),
                        node("ev", "event.initialize"),
                        node("ret", "exec.return"),
                        node("add", "op.add"),
                        node("va", "variable", opts("name", "a")),
                        node("c1", "const.number", opts("value", 1))),
                List.of(wire("ev", "exec_out", "ret", "exec_in"),
                        wire("add", "out", "ret", "value"),
                        wire("va", "out", "add", "a"),
                        wire("c1", "out", "add", "b")));
        assertEquals("return (variable.a + 1)", exec(library(main), "ev"));
    }

    // ---------- 子图内联展开（D5/§2.4-9） ----------

    /** 子图 dbl：input x → add(x, x) → output.result。 */
    private static GraphData dblSubgraph() {
        GraphInterface iface = new GraphInterface(
                List.of(GraphInterface.Param.of("x", PortType.FLOAT)),
                GraphInterface.Param.of("result", PortType.FLOAT));
        return subgraph(iface,
                List.of(node("in", "subgraph.input"),
                        node("add", "op.add"),
                        node("out", "subgraph.output")),
                List.of(wire("in", "x", "add", "a"),
                        wire("in", "x", "add", "b"),
                        wire("add", "out", "out", "result")));
    }

    @Test
    void subgraphInlineExtractsSharedArgumentWithNameIsolation() {
        // 实参 (query.a + 1) 非平凡且被子图引用 2 次 → temp.sg0_x 提取
        GraphData main = graph(
                List.of(root(),
                        node("call", "subgraph.call", opts("subgraph", "dbl")),
                        node("arg", "op.add"),
                        node("qa", "query.call", opts("function", "query.a", "arg_count", 0)),
                        node("c1", "const.number", opts("value", 1))),
                List.of(wire("call", "result", "root", "scale"),
                        wire("arg", "out", "call", "x"),
                        wire("qa", "out", "arg", "a"),
                        wire("c1", "out", "arg", "b")));
        GraphLibrary lib = library(main, Map.of("dbl", dblSubgraph()));
        assertEquals("temp.sg0_x = (query.a + 1); (temp.sg0_x + temp.sg0_x)", expr(lib, "scale"));
    }

    @Test
    void subgraphInnerSharedTempUsesIsolatedPrefix() {
        // 子图 sq：input x → add(x+1)，mul(add,add) → 内部共享子表达式提取 temp.sg0_g0
        GraphInterface iface = new GraphInterface(
                List.of(GraphInterface.Param.of("x", PortType.FLOAT)),
                GraphInterface.Param.of("result", PortType.FLOAT));
        GraphData sq = subgraph(iface,
                List.of(node("in", "subgraph.input"),
                        node("add", "op.add"),
                        node("c1", "const.number", opts("value", 1)),
                        node("mul", "op.multiply"),
                        node("out", "subgraph.output")),
                List.of(wire("in", "x", "add", "a"),
                        wire("c1", "out", "add", "b"),
                        wire("add", "out", "mul", "a"),
                        wire("add", "out", "mul", "b"),
                        wire("mul", "out", "out", "result")));
        GraphData main = graph(
                List.of(root(),
                        node("call", "subgraph.call", opts("subgraph", "sq")),
                        node("w", "variable", opts("name", "w"))),
                List.of(wire("call", "result", "root", "scale"),
                        wire("w", "out", "call", "x")));
        assertEquals("temp.sg0_g0 = (variable.w + 1); (temp.sg0_g0 * temp.sg0_g0)",
                expr(library(main, Map.of("sq", sq)), "scale"));
    }

    @Test
    void subgraphExecChainRunsBeforeResult() {
        // 子图 out 锚点 exec_in 链（variable.b = 2）先于 result 发射
        GraphInterface iface = new GraphInterface(
                List.of(GraphInterface.Param.of("x", PortType.FLOAT)),
                GraphInterface.Param.of("result", PortType.FLOAT));
        GraphData sub = subgraph(iface,
                List.of(node("in", "subgraph.input"),
                        node("set", "exec.set_var"),
                        node("tb", "variable", opts("name", "b")),
                        node("c2", "const.number", opts("value", 2)),
                        node("add", "op.add"),
                        node("c1", "const.number", opts("value", 1)),
                        node("out", "subgraph.output")),
                List.of(wire("set", "exec_out", "out", "exec_in"),
                        wire("set", "target", "tb", "in"),
                        wire("c2", "out", "set", "value"),
                        wire("in", "x", "add", "a"),
                        wire("c1", "out", "add", "b"),
                        wire("add", "out", "out", "result")));
        GraphData main = graph(
                List.of(root(),
                        node("call", "subgraph.call", opts("subgraph", "side")),
                        node("q", "query.call", opts("function", "query.q", "arg_count", 0))),
                List.of(wire("call", "result", "root", "scale"),
                        wire("q", "out", "call", "x")));
        assertEquals("variable.b = 2; (query.q + 1)", expr(library(main, Map.of("side", sub)), "scale"));
    }

    @Test
    void subgraphRecursionIsError() {
        GraphInterface iface = new GraphInterface(
                List.of(GraphInterface.Param.of("x", PortType.FLOAT)),
                GraphInterface.Param.of("result", PortType.FLOAT));
        // 子图 self 内调用自身
        GraphData self = subgraph(iface,
                List.of(node("in", "subgraph.input"),
                        node("call", "subgraph.call", opts("subgraph", "self")),
                        node("out", "subgraph.output")),
                List.of(wire("in", "x", "call", "x"),
                        wire("call", "result", "out", "result")));
        GraphData main = graph(
                List.of(root(),
                        node("call", "subgraph.call", opts("subgraph", "self")),
                        node("c1", "const.number", opts("value", 1))),
                List.of(wire("call", "result", "root", "scale"),
                        wire("c1", "out", "call", "x")));
        CodegenResult r = new MolangGenerator(library(main, Map.of("self", self)))
                .emitExpressionFor("root", "root", "scale");
        assertTrue(r.hasErrors());
        assertTrue(r.diagnostics().stream().anyMatch(d -> d.code().equals("SUBGRAPH_RECURSION")),
                () -> "diagnostics: " + r.diagnostics());
    }

    // ---------- 不变量 ----------

    @Test
    void emissionIsDeterministic() {
        GraphData main = graph(
                List.of(root(),
                        node("add", "op.add"),
                        node("x", "query.call", opts("function", "query.x", "arg_count", 0)),
                        node("one", "const.number", opts("value", 1)),
                        node("mul", "op.multiply")),
                List.of(wire("mul", "out", "root", "scale"),
                        wire("x", "out", "add", "a"),
                        wire("one", "out", "add", "b"),
                        wire("add", "out", "mul", "a"),
                        wire("add", "out", "mul", "b")));
        GraphLibrary lib = library(main);
        String first = new MolangGenerator(lib).emitExpressionFor("root", "root", "scale").code();
        String second = new MolangGenerator(lib).emitExpressionFor("root", "root", "scale").code();
        assertEquals(first, second);
    }

    @Test
    void invalidSlotsProduceDiagnosticsNotExceptions() {
        GraphData main = graph(List.of(root(), node("ev", "event.initialize")), List.of());
        MolangGenerator gen = new MolangGenerator(library(main));
        // 值槽上请求语句序列 / EXEC OUT 源端口上请求表达式 / 未知图 / 未知节点
        assertTrue(gen.emitStatementListFromFor("root", "root", "scale").hasErrors());
        assertTrue(gen.emitExpressionFor("root", "ev", "exec_out").hasErrors());
        assertTrue(gen.emitExpression("nope", new PortRef("root", "scale")).hasErrors());
        assertTrue(gen.emitExpressionFor("root", "ghost", "scale").hasErrors());
        Diagnostic d = gen.emitExpression("nope", new PortRef("root", "scale")).diagnostics().get(0);
        assertEquals("UNKNOWN_GRAPH", d.code());
    }
}
