package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import io.github.tt432.eyelib.nodegraph.Wire;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.opts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link InitDefaultFolder} 单测：只断言可观测契约——折叠后节点/连线/声明默认值形态与
 * 「不满足条件即原样保留」的负例。
 */
class InitDefaultFolderTest {

    private static NodeInstance node(String uid, String type) {
        return new NodeInstance(uid, type, 0, 0, Map.of(), Map.of());
    }

    private static NodeInstance node(String uid, String type, Map<String, JsonElement> options) {
        return new NodeInstance(uid, type, 0, 0, options, Map.of());
    }

    private static Wire wire(String fromNode, String fromPort, String toNode, String toPort) {
        return new Wire(new PortRef(fromNode, fromPort), new PortRef(toNode, toPort));
    }

    private static boolean hasNode(InitDefaultFolder.Result r, String uid) {
        return r.nodes().stream().anyMatch(n -> n.uid().equals(uid));
    }

    private static boolean hasWire(InitDefaultFolder.Result r, String fromNode, String fromPort,
                                   String toNode, String toPort) {
        return r.wires().stream().anyMatch(w -> w.from().node().equals(fromNode)
                && w.from().port().equals(fromPort)
                && w.to().node().equals(toNode) && w.to().port().equals(toPort));
    }

    private static Optional<VariableDecl> decl(InitDefaultFolder.Result r, String name) {
        return r.foldedDecls().stream().filter(d -> d.name().equals(name)).findFirst();
    }

    private static InitDefaultFolder.Result fold(List<NodeInstance> nodes, List<Wire> wires) {
        return InitDefaultFolder.fold(nodes, wires, List.of());
    }

    /** 正例：event.initialize 链上 value 未连线（默认 0）→ 折叠为默认值 0，节点删除。 */
    @Test
    void foldsUnwiredConstantIntoDefault() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("ev", "event.initialize"),
                node("v", "variable", opts("name", "x")),
                node("s", "exec.set_var")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "target", "v", "in"),
                wire("ev", "exec_out", "s", "exec_in")));

        InitDefaultFolder.Result r = fold(nodes, wires);

        assertFalse(hasNode(r, "v"));
        assertFalse(hasNode(r, "s"));
        assertTrue(hasNode(r, "ev")); // 事件锚点保留，exec_out 悬空
        assertFalse(hasWire(r, "ev", "exec_out", "s", "exec_in"));
        assertEquals(1, r.foldedDecls().size());
        VariableDecl d = decl(r, "x").orElseThrow();
        assertEquals(new JsonPrimitive(0), d.defaultValue().orElseThrow());
        assertTrue(DecompileTestSupport.hasCode(r.diagnostics(), InitDefaultFolder.INIT_DEFAULT_FOLD));
    }

    /** decl 声明节点与写入节点是独立实例、且接线在导入后处理（折叠/内联之后），
     * 结构上不与折叠互斥：折叠只删写入节点，声明节点/连线原样保留。 */
    @Test
    void foldLeavesDeclNodesIntact() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("ev", "event.initialize"),
                node("v", "variable", opts("name", "x")),
                node("dv", "variable", opts("name", "y")),
                node("ra", "ref.animation"),
                node("s", "exec.set_var")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "target", "v", "in"),
                wire("dv", "out", "ra", "read:y"),
                wire("ev", "exec_out", "s", "exec_in")));

        InitDefaultFolder.Result r = fold(nodes, wires);

        assertFalse(hasNode(r, "v"));
        assertFalse(hasNode(r, "s"));
        assertTrue(hasNode(r, "dv")); // 声明节点不动
        assertTrue(hasWire(r, "dv", "out", "ra", "read:y"));
        assertTrue(decl(r, "x").isPresent());
    }

    /** 正例：两个 set_var 串联（ev→s1→s2，链尾悬空），各自折叠且 exec 链逐步塌缩。 */
    @Test
    void foldsChainedInitStatements() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("ev", "event.initialize"),
                node("v1", "variable", opts("name", "a")),
                node("v2", "variable", opts("name", "b")),
                node("s1", "exec.set_var"),
                node("s2", "exec.set_var")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s1", "target", "v1", "in"),
                wire("s2", "target", "v2", "in"),
                wire("ev", "exec_out", "s1", "exec_in"),
                wire("s1", "exec_out", "s2", "exec_in")));

        InitDefaultFolder.Result r = fold(nodes, wires);

        assertEquals(2, r.foldedDecls().size());
        assertTrue(decl(r, "a").isPresent());
        assertTrue(decl(r, "b").isPresent());
        assertFalse(hasNode(r, "s1"));
        assertFalse(hasNode(r, "s2"));
        assertTrue(hasNode(r, "ev")); // 事件锚点保留
    }

    /** 正例：值连线自 const.int → 用其常量，孤儿 const 节点一并删除。 */
    @Test
    void foldsConstWiredValueAndRemovesOrphanConst() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("ev", "event.initialize"),
                node("v", "variable", opts("name", "x")),
                node("s", "exec.set_var"),
                node("c", "const.int", opts("value", 7))));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "target", "v", "in"),
                wire("c", "out", "s", "value"),
                wire("ev", "exec_out", "s", "exec_in")));

        InitDefaultFolder.Result r = fold(nodes, wires);

        assertFalse(hasNode(r, "c"));
        assertEquals(new JsonPrimitive(7), decl(r, "x").orElseThrow().defaultValue().orElseThrow());
        assertEquals(io.github.tt432.eyelib.nodegraph.PortType.INT,
                decl(r, "x").orElseThrow().type());
    }

    /** 负例：读取在同一 initialize 链上且先于写入（读 x 喂给链上前置 set_var）→ 不折。 */
    @Test
    void skipsWhenReadPrecedesWriteOnInitChain() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("ev", "event.initialize"),
                node("v", "variable", opts("name", "x")),
                node("vy", "variable", opts("name", "y")),
                node("sRead", "exec.set_var"),
                node("s", "exec.set_var")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "target", "v", "in"),
                wire("sRead", "target", "vy", "in"),
                wire("v", "out", "sRead", "value"),
                wire("ev", "exec_out", "sRead", "exec_in"),
                wire("sRead", "exec_out", "s", "exec_in")));

        InitDefaultFolder.Result r = fold(nodes, wires);

        assertTrue(r.foldedDecls().isEmpty());
        assertTrue(hasNode(r, "s"));
    }

    /** 正例：读取在纯值语境（无 exec 下游，如 RC 表达式）→ initialize 完成后才求值，折叠。 */
    @Test
    void foldsWhenReadInValueOnlyContext() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("ev", "event.initialize"),
                node("v", "variable", opts("name", "x")),
                node("s", "exec.set_var"),
                node("q", "query.call", opts("function", "query.health", "arg_count", 0))));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "target", "v", "in"),
                wire("v", "out", "q", "arg1"),
                wire("ev", "exec_out", "s", "exec_in")));

        InitDefaultFolder.Result r = fold(nodes, wires);

        assertEquals(1, r.foldedDecls().size());
        assertFalse(hasNode(r, "s"));
        assertTrue(hasNode(r, "v")); // 自带读边 → 保留为纯读节点
        assertTrue(hasWire(r, "v", "out", "q", "arg1")); // 读边不动
    }

    /** 正例（qpptaw 模式）：同名只读节点挂在 pre_animation 链上 → 读取必在 initialize
     * 完成后，折叠照常；读节点与读边原样保留（声明默认值承接写入）。 */
    @Test
    void foldsWhenReadOnPreAnimationChain() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("ev", "event.initialize"),
                node("evPre", "event.pre_animation"),
                node("v", "variable", opts("name", "x")),
                node("vRead", "variable", opts("name", "x")),
                node("vy", "variable", opts("name", "y")),
                node("s", "exec.set_var"),
                node("sCopy", "exec.set_var")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "target", "v", "in"),
                wire("ev", "exec_out", "s", "exec_in"),
                wire("vRead", "out", "sCopy", "value"),
                wire("sCopy", "target", "vy", "in"),
                wire("evPre", "exec_out", "sCopy", "exec_in")));

        InitDefaultFolder.Result r = fold(nodes, wires);

        VariableDecl d = decl(r, "x").orElseThrow();
        assertEquals(new JsonPrimitive(0), d.defaultValue().orElseThrow());
        assertFalse(hasNode(r, "v"));
        assertFalse(hasNode(r, "s"));
        assertTrue(hasNode(r, "vRead")); // 读取节点保留，照常读声明默认值
        assertTrue(hasWire(r, "vRead", "out", "sCopy", "value"));
        assertTrue(hasWire(r, "evPre", "exec_out", "sCopy", "exec_in"));
        // y 的写在 pre_animation 链且值非常量 → 不折
        assertTrue(decl(r, "y").isEmpty());
        assertTrue(hasNode(r, "sCopy"));
    }

    /** 正例：读取在同一 initialize 链上但位于写入之后（读可达）→ 折叠；
     * 写入侧 variable 节点自带读边时保留为纯读节点。 */
    @Test
    void foldsWhenReadFollowsWriteOnInitChain() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("ev", "event.initialize"),
                node("v", "variable", opts("name", "x")),
                node("vy", "variable", opts("name", "y")),
                node("s", "exec.set_var"),
                node("sRead", "exec.set_var")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "target", "v", "in"),
                wire("sRead", "target", "vy", "in"),
                wire("v", "out", "sRead", "value"),
                wire("ev", "exec_out", "s", "exec_in"),
                wire("s", "exec_out", "sRead", "exec_in")));

        InitDefaultFolder.Result r = fold(nodes, wires);

        assertTrue(decl(r, "x").isPresent());
        assertFalse(hasNode(r, "s"));
        assertTrue(hasNode(r, "v")); // 自带读边 → 保留为纯读节点
        assertTrue(hasWire(r, "v", "out", "sRead", "value")); // 读边不动
        // exec 旁路：ev → sRead 直连
        assertTrue(hasWire(r, "ev", "exec_out", "sRead", "exec_in"));
        // y 的值仍是变量读取（非常量）→ 不折
        assertTrue(decl(r, "y").isEmpty());
    }

    /** 正例：同名孤立 variable 节点（无写无读）不阻挡折叠。 */
    @Test
    void foldsDespiteOrphanSameNameNode() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("ev", "event.initialize"),
                node("v1", "variable", opts("name", "x")),
                node("v2", "variable", opts("name", "x")),
                node("s", "exec.set_var")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "target", "v1", "in"),
                wire("ev", "exec_out", "s", "exec_in")));

        InitDefaultFolder.Result r = fold(nodes, wires);

        assertEquals(1, r.foldedDecls().size());
        assertFalse(hasNode(r, "v1"));
        assertFalse(hasNode(r, "s"));
        assertTrue(hasNode(r, "v2")); // 孤立节点保留
    }

    /** 负例：同名节点带第二个写入通道（多处写入）→ 不折。 */
    @Test
    void skipsWhenSecondWriteExists() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("ev", "event.initialize"),
                node("v1", "variable", opts("name", "x")),
                node("v2", "variable", opts("name", "x")),
                node("s1", "exec.set_var"),
                node("s2", "exec.set_var")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s1", "target", "v1", "in"),
                wire("s2", "target", "v2", "in"),
                wire("ev", "exec_out", "s1", "exec_in"),
                wire("s1", "exec_out", "s2", "exec_in")));

        InitDefaultFolder.Result r = fold(nodes, wires);

        assertTrue(r.foldedDecls().isEmpty());
    }

    /** 负例：值连线自非 const 节点（表达式）→ 不折。 */
    @Test
    void skipsExpressionValue() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("ev", "event.initialize"),
                node("v", "variable", opts("name", "x")),
                node("s", "exec.set_var"),
                node("m", "op.add")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "target", "v", "in"),
                wire("m", "out", "s", "value"),
                wire("ev", "exec_out", "s", "exec_in")));

        InitDefaultFolder.Result r = fold(nodes, wires);

        assertTrue(r.foldedDecls().isEmpty());
    }

    /** 负例：链挂在 event.pre_animation 而非 event.initialize → 不折（导出位置会变，严格等价不折）。 */
    @Test
    void skipsPreAnimationTerminal() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("ev", "event.pre_animation"),
                node("v", "variable", opts("name", "x")),
                node("s", "exec.set_var")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "target", "v", "in"),
                wire("ev", "exec_out", "s", "exec_in")));

        InitDefaultFolder.Result r = fold(nodes, wires);

        assertTrue(r.foldedDecls().isEmpty());
        assertTrue(hasNode(r, "s"));
    }

    /** 负例：锚点到 set_var 的路径中间夹 exec.call（副作用语句）→ 不折。 */
    @Test
    void skipsWhenCallBetween() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("ev", "event.initialize"),
                node("v", "variable", opts("name", "x")),
                node("s", "exec.set_var"),
                node("call", "exec.call", opts("function", "query.foo", "arg_count", 0))));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "target", "v", "in"),
                wire("ev", "exec_out", "call", "exec_in"),
                wire("call", "exec_out", "s", "exec_in")));

        InitDefaultFolder.Result r = fold(nodes, wires);

        assertTrue(r.foldedDecls().isEmpty());
    }

    /** 负例：变量名出现在行内 molang 文本（variable.x）→ 不折。 */
    @Test
    void skipsTextualReference() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("ev", "event.initialize"),
                node("v", "variable", opts("name", "x")),
                node("s", "exec.set_var"),
                node("q", "query.call", opts("function", "math.min", "arg_count", 2)),
                new NodeInstance("lit", "exec.set_var", 0, 0, Map.of(),
                        Map.of("value", new JsonPrimitive("variable.x + 1")))));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "target", "v", "in"),
                wire("ev", "exec_out", "s", "exec_in"),
                wire("s", "exec_out", "lit", "exec_in")));

        InitDefaultFolder.Result r = fold(nodes, wires);

        assertTrue(r.foldedDecls().isEmpty());
    }

    /** 负例：链未接入任何执行时机锚点（链首 exec_in 悬空，dead-end）→ 折叠会新增从未执行的语句，不折。 */
    @Test
    void skipsDeadEndChain() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("v", "variable", opts("name", "x")),
                node("s", "exec.set_var")));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "target", "v", "in")));

        InitDefaultFolder.Result r = fold(nodes, wires);

        assertTrue(r.foldedDecls().isEmpty());
    }

    /** 声明冲突：已有不同默认值 → 不折；相同默认值 → 照常折。 */
    @Test
    void respectsExistingDefaultConflict() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("ev", "event.initialize"),
                node("v", "variable", opts("name", "x")),
                node("s", "exec.set_var"),
                node("c", "const.int", opts("value", 7))));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("s", "target", "v", "in"),
                wire("c", "out", "s", "value"),
                wire("ev", "exec_out", "s", "exec_in")));

        InitDefaultFolder.Result conflict = InitDefaultFolder.fold(nodes, wires, List.of(
                new VariableDecl("x", io.github.tt432.eyelib.nodegraph.PortType.INT,
                        Optional.empty(), Optional.of(new JsonPrimitive(3)),
                        VariableDecl.Scope.VARIABLE)));
        assertTrue(conflict.foldedDecls().isEmpty());

        InitDefaultFolder.Result same = InitDefaultFolder.fold(nodes, wires, List.of(
                new VariableDecl("x", io.github.tt432.eyelib.nodegraph.PortType.INT,
                        Optional.empty(), Optional.of(new JsonPrimitive(7)),
                        VariableDecl.Scope.VARIABLE)));
        assertEquals(1, same.foldedDecls().size());
    }

    /** 类型推断：bool→BOOL、小数→FLOAT、字符串→STRING。 */
    @Test
    void infersDeclTypeFromConstant() {
        List<NodeInstance> nodes = new ArrayList<>(List.of(
                node("root", "entity.root"),
                node("ev", "event.initialize"),
                node("vb", "variable", opts("name", "flag")),
                node("sb", "exec.set_var"),
                node("cb", "const.bool", opts("value", true))));
        List<Wire> wires = new ArrayList<>(List.of(
                wire("sb", "target", "vb", "in"),
                wire("cb", "out", "sb", "value"),
                wire("ev", "exec_out", "sb", "exec_in")));

        InitDefaultFolder.Result r = fold(nodes, wires);

        assertEquals(io.github.tt432.eyelib.nodegraph.PortType.BOOL,
                decl(r, "flag").orElseThrow().type());
    }
}
