package io.github.tt432.eyelib.nodegraph.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.EmolangParser;
import io.github.tt432.eyelib.nodegraph.EmolangRegistry;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * .emolang 自定义函数 codegen 展开单测（规格 nodegraph-emolang-functions §4/§5）：
 * 内联展开、temp 卫生重命名、实参提取、嵌套展开、递归与 arity 诊断、未知裸名警告。
 */
class EmolangCodegenTest {

    @AfterEach
    void 清空注册表() {
        EmolangRegistry.replaceAll(List.of());
    }

    // ---------- 构造辅助（对齐 MolangGeneratorTest 风格） ----------

    private static NodeInstance node(String uid, String type) {
        return NodeInstance.of(uid, type, 0, 0);
    }

    private static NodeInstance node(String uid, String type, Map<String, JsonElement> options) {
        return new NodeInstance(uid, type, 0, 0, options, Map.of());
    }

    private static Map<String, JsonElement> opts(Object... kv) {
        Map<String, JsonElement> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            JsonElement e = v instanceof String s ? new JsonPrimitive(s)
                    : v instanceof Number n ? new JsonPrimitive(n)
                    : (JsonElement) v;
            map.put((String) kv[i], e);
        }
        return map;
    }

    private static Wire wire(String fromNode, String fromPort, String toNode, String toPort) {
        return new Wire(new PortRef(fromNode, fromPort), new PortRef(toNode, toPort));
    }

    private static GraphLibrary library(List<NodeInstance> nodes, List<Wire> wires) {
        GraphData main = new GraphData(nodes, wires, List.of(), List.of(), List.of(), Optional.empty());
        return new GraphLibrary(1, GraphKind.CLIENT_ENTITY, "root", Map.of("root", main));
    }

    private static void register(String source) {
        var r = EmolangParser.parse("test.emolang", source);
        if (r.function() == null) {
            throw new IllegalArgumentException("测试函数解析失败: " + r.error());
        }
        EmolangRegistry.replaceAll(List.of(r.function()));
    }

    private static CodegenResult emitExpr(GraphLibrary lib) {
        return new MolangGenerator(lib).emitExpressionFor("root", "root", "scale");
    }

    /** const.number 值节点。 */
    private static NodeInstance num(String uid, double value) {
        return node(uid, "const.number", opts("value", value));
    }

    // ---------- 展开 ----------

    @Test
    void 基本内联展开() {
        register("function custom1(arg1: int, arg2: float) { return arg1 + arg2; }");
        GraphLibrary lib = library(
                List.of(node("root", "entity.root"),
                        node("c", "query.call", opts("function", "custom1")),
                        num("one", 1), num("two", 2)),
                List.of(wire("c", "out", "root", "scale"),
                        wire("one", "out", "c", "arg1"),
                        wire("two", "out", "c", "arg2")));
        CodegenResult r = emitExpr(lib);
        assertFalse(r.hasErrors(), () -> "errors: " + r.diagnostics());
        assertEquals("(1) + (2)", r.code());
    }

    @Test
    void 局部变量卫生重命名() {
        register("function f(a) { temp.d = a * 2; return temp.d + a; }");
        GraphLibrary lib = library(
                List.of(node("root", "entity.root"),
                        node("c", "query.call", opts("function", "f")),
                        num("one", 3)),
                List.of(wire("c", "out", "root", "scale"),
                        wire("one", "out", "c", "arg1")));
        CodegenResult r = emitExpr(lib);
        assertFalse(r.hasErrors(), () -> "errors: " + r.diagnostics());
        // temp.d → temp.em<K>_d；a 引用 2 次但 3 是字面量可复制 → 不提取
        assertEquals("temp.em0_d = (3) * 2; temp.em0_d + (3)", r.code());
    }

    @Test
    void 多次引用的非平凡实参提取temp() {
        register("function sq(x) { return x * x; }");
        GraphLibrary lib = library(
                List.of(node("root", "entity.root"),
                        node("c", "query.call", opts("function", "sq")),
                        node("h", "query.call", opts("function", "query.health")),
                        node("plus", "op.binary", opts("op", "+")),
                        num("one", 1)),
                List.of(wire("c", "out", "root", "scale"),
                        wire("plus", "out", "c", "arg1"),
                        wire("h", "out", "plus", "a"),
                        wire("one", "out", "plus", "b")));
        CodegenResult r = emitExpr(lib);
        assertFalse(r.hasErrors(), () -> "errors: " + r.diagnostics());
        assertEquals("temp.em0_x = (query.health + 1); temp.em0_x * temp.em0_x", r.code());
    }

    @Test
    void 别名t的局部变量同样重命名() {
        register("function f(a) { t.q = a; return t.q; }");
        GraphLibrary lib = library(
                List.of(node("root", "entity.root"),
                        node("c", "query.call", opts("function", "f")),
                        num("one", 7)),
                List.of(wire("c", "out", "root", "scale"),
                        wire("one", "out", "c", "arg1")));
        CodegenResult r = emitExpr(lib);
        assertFalse(r.hasErrors(), () -> "errors: " + r.diagnostics());
        assertEquals("temp.em0_q = (7); temp.em0_q", r.code());
    }

    @Test
    void 嵌套自定义函数展开() {
        var inner = EmolangParser.parse("inner.emolang", "function dbl(x) { return x * 2; }");
        var outer = EmolangParser.parse("outer.emolang", "function f(y) { return dbl(y) + 1; }");
        assert inner.function() != null && outer.function() != null;
        EmolangRegistry.replaceAll(List.of(inner.function(), outer.function()));
        GraphLibrary lib = library(
                List.of(node("root", "entity.root"),
                        node("c", "query.call", opts("function", "f")),
                        num("three", 3)),
                List.of(wire("c", "out", "root", "scale"),
                        wire("three", "out", "c", "arg1")));
        CodegenResult r = emitExpr(lib);
        assertFalse(r.hasErrors(), () -> "errors: " + r.diagnostics());
        assertEquals("(((3)) * 2) + 1", r.code());
    }

    @Test
    void 自递归报诊断() {
        register("function bad(x) { return bad(x); }");
        GraphLibrary lib = library(
                List.of(node("root", "entity.root"),
                        node("c", "query.call", opts("function", "bad")),
                        num("one", 1)),
                List.of(wire("c", "out", "root", "scale"),
                        wire("one", "out", "c", "arg1")));
        CodegenResult r = emitExpr(lib);
        assertTrue(r.hasErrors());
        assertTrue(r.diagnostics().stream().anyMatch(d -> d.code().equals("EMOLANG_RECURSION")),
                () -> "diagnostics: " + r.diagnostics());
    }

    @Test
    void 实参不足补零并警告() {
        register("function add2(a, b) { return a + b; }");
        GraphLibrary lib = library(
                List.of(node("root", "entity.root"),
                        node("c", "query.call", opts("function", "add2")),
                        num("one", 1)),
                List.of(wire("c", "out", "root", "scale"),
                        wire("one", "out", "c", "arg1")));
        CodegenResult r = emitExpr(lib);
        assertFalse(r.hasErrors(), () -> "errors: " + r.diagnostics());
        assertTrue(r.diagnostics().stream().anyMatch(d -> d.code().equals("EMOLANG_ARITY")),
                () -> "diagnostics: " + r.diagnostics());
        assertEquals("(1) + (0)", r.code());
    }

    @Test
    void 未知裸名警告并原样直发() {
        GraphLibrary lib = library(
                List.of(node("root", "entity.root"),
                        node("c", "query.call", opts("function", "not_a_function"))),
                List.of(wire("c", "out", "root", "scale")));
        CodegenResult r = emitExpr(lib);
        assertFalse(r.hasErrors(), () -> "errors: " + r.diagnostics());
        assertTrue(r.diagnostics().stream().anyMatch(d -> d.code().equals("UNKNOWN_FUNCTION")),
                () -> "diagnostics: " + r.diagnostics());
        assertEquals("not_a_function", r.code());
    }

    @Test
    void execCall语句位展开() {
        register("function sethp(val) { variable.hp = val; return 0; }");
        GraphLibrary lib = library(
                List.of(node("root", "entity.root"),
                        node("ev", "event.initialize"),
                        node("c", "exec.call", opts("function", "sethp")),
                        num("nine", 9)),
                List.of(wire("ev", "exec_out", "c", "exec_in"),
                        wire("nine", "out", "c", "arg1")));
        CodegenResult r = new MolangGenerator(lib).emitStatementListFromFor("root", "ev", "exec_out");
        assertFalse(r.hasErrors(), () -> "errors: " + r.diagnostics());
        assertTrue(r.code().contains("variable.hp = (9)"), () -> r.code());
    }

    @Test
    void 注册函数驱动端口签名() {
        register("function custom1(arg1: int, arg2: string) { return arg1; }");
        var type = io.github.tt432.eyelib.nodegraph.NodeTypes.QUERY_CALL;
        var instance = new NodeInstance("u", "query.call", 0, 0,
                Map.of("function", new JsonPrimitive("custom1")), Map.of());
        var ports = type.inputsOf(instance, name -> Optional.empty());
        assertEquals(2, ports.size());
        assertEquals("arg1", ports.get(0).label().orElseThrow());
        assertEquals("arg2: str", ports.get(1).label().orElseThrow());
    }
}
