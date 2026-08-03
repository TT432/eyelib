package io.github.tt432.eyelib.nodegraph.decompile;

import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.allByType;
import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.countCode;
import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.firstByType;
import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.hasCode;
import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.wireInto;
import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.wireSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortRef;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link MolangDecompiler} 单测：规格 §W2 映射表逐行黄金断言（AST → 节点/连线）、
 * 别名归一、exec 链 wire 方向、不支持结构（诊断 + 便签 + const 0 占位，其余不中断）。
 */
class MolangDecompilerTest {

    // ---------- 字面量 ----------

    @Test
    void numberLiterals() {
        MolangDecompiler.ExprFragment decimal = MolangDecompiler.decompileExpression("1.5");
        NodeInstance number = firstByType(decimal.nodes(), "const.number");
        assertEquals(1.5, number.options().get("value").getAsDouble());
        assertEquals(new PortRef(number.uid(), "out"), decimal.output().orElseThrow());

        MolangDecompiler.ExprFragment integral = MolangDecompiler.decompileExpression("5");
        NodeInstance intNode = firstByType(integral.nodes(), "const.int");
        assertEquals(5, intNode.options().get("value").getAsInt());

        // 科学计数法不是整数形 → const.number
        MolangDecompiler.ExprFragment scientific = MolangDecompiler.decompileExpression("1e3");
        assertEquals(1000.0, firstByType(scientific.nodes(), "const.number")
                .options().get("value").getAsDouble());
    }

    @Test
    void stringLiteral() {
        MolangDecompiler.ExprFragment f = MolangDecompiler.decompileExpression("'hello'");
        assertEquals("hello", firstByType(f.nodes(), "const.string")
                .options().get("value").getAsString());
    }

    @Test
    void boolLiterals() {
        assertEquals(true, firstByType(MolangDecompiler.decompileExpression("true").nodes(),
                "const.bool").options().get("value").getAsBoolean());
        assertEquals(false, firstByType(MolangDecompiler.decompileExpression("false").nodes(),
                "const.bool").options().get("value").getAsBoolean());
    }

    // ---------- 运算符 ----------

    @Test
    void binaryOperator() {
        MolangDecompiler.ExprFragment f = MolangDecompiler.decompileExpression("1 + variable.x");
        NodeInstance op = firstByType(f.nodes(), "op.binary");
        assertEquals("+", op.options().get("op").getAsString());
        assertEquals(new PortRef(op.uid(), "out"), f.output().orElseThrow());

        NodeInstance a = firstByType(f.nodes(), "const.int");
        NodeInstance b = firstByType(f.nodes(), "var.get");
        assertEquals(a.uid(), wireSource(f.wires(), op.uid(), "a"));
        assertEquals(b.uid(), wireSource(f.wires(), op.uid(), "b"));
        assertFalse(f.diagnostics().stream().anyMatch(d -> true));
    }

    @Test
    void unaryOperator() {
        MolangDecompiler.ExprFragment neg = MolangDecompiler.decompileExpression("-variable.a");
        NodeInstance op = firstByType(neg.nodes(), "op.unary");
        assertEquals("-", op.options().get("op").getAsString());
        assertEquals(firstByType(neg.nodes(), "var.get").uid(), wireSource(neg.wires(), op.uid(), "a"));

        MolangDecompiler.ExprFragment not = MolangDecompiler.decompileExpression("!query.is_baby");
        assertEquals("!", firstByType(not.nodes(), "op.unary").options().get("op").getAsString());
    }

    @Test
    void ternaryAndNullCoalesce() {
        MolangDecompiler.ExprFragment t = MolangDecompiler.decompileExpression("query.is_baby ? 1 : 2");
        NodeInstance ternary = firstByType(t.nodes(), "op.ternary");
        assertEquals(firstByType(t.nodes(), "query.call").uid(), wireSource(t.wires(), ternary.uid(), "cond"));
        assertTrue(wireInto(t.wires(), ternary.uid(), "a").isPresent());
        assertTrue(wireInto(t.wires(), ternary.uid(), "b").isPresent());

        MolangDecompiler.ExprFragment nc = MolangDecompiler.decompileExpression("variable.a ?? 0");
        NodeInstance coalesce = firstByType(nc.nodes(), "op.null_coalesce");
        assertEquals(firstByType(nc.nodes(), "var.get").uid(), wireSource(nc.wires(), coalesce.uid(), "a"));
    }

    @Test
    void groupingDissolves() {
        // 图 IR 无括号节点：(1 + 2) * 3 与 1 + 2 * 3 的 AST 不同但结构映射一致
        MolangDecompiler.ExprFragment f = MolangDecompiler.decompileExpression("(1 + 2) * 3");
        List<NodeInstance> binaries = allByType(f.nodes(), "op.binary");
        assertEquals(2, binaries.size());
        NodeInstance mul = binaries.stream()
                .filter(n -> "*".equals(n.options().get("op").getAsString())).findFirst().orElseThrow();
        NodeInstance add = binaries.stream()
                .filter(n -> "+".equals(n.options().get("op").getAsString())).findFirst().orElseThrow();
        assertEquals(add.uid(), wireSource(f.wires(), mul.uid(), "a"));
    }

    // ---------- 变量/根与别名 ----------

    @Test
    void variableRootsAndAliases() {
        assertEquals("variable.x", firstByType(MolangDecompiler.decompileExpression("variable.x")
                .nodes(), "var.get").options().get("name").getAsString());
        // v. 别名归一为 variable.
        assertEquals("variable.x", firstByType(MolangDecompiler.decompileExpression("v.x")
                .nodes(), "var.get").options().get("name").getAsString());
        assertEquals("temp.t", firstByType(MolangDecompiler.decompileExpression("t.t")
                .nodes(), "temp.get").options().get("name").getAsString());
        assertEquals("context.other", firstByType(MolangDecompiler.decompileExpression("c.other")
                .nodes(), "context.get").options().get("name").getAsString());
        // 多级成员路径保留
        assertEquals("variable.foo.bar", firstByType(MolangDecompiler.decompileExpression("variable.foo.bar")
                .nodes(), "var.get").options().get("name").getAsString());
    }

    // ---------- query/math 调用 ----------

    @Test
    void queryCallWithArgs() {
        MolangDecompiler.ExprFragment f =
                MolangDecompiler.decompileExpression("query.get_name(1, 'a')");
        NodeInstance call = firstByType(f.nodes(), "query.call");
        assertEquals("query.get_name", call.options().get("function").getAsString());
        assertEquals(2, call.options().get("arg_count").getAsInt());
        assertEquals(firstByType(f.nodes(), "const.int").uid(), wireSource(f.wires(), call.uid(), "arg1"));
        assertEquals(firstByType(f.nodes(), "const.string").uid(), wireSource(f.wires(), call.uid(), "arg2"));
    }

    @Test
    void queryMemberWithoutCall() {
        // 无参成员访问形 → arg_count=0 的 query.call；q. 别名归一
        MolangDecompiler.ExprFragment f = MolangDecompiler.decompileExpression("q.anim_time");
        NodeInstance call = firstByType(f.nodes(), "query.call");
        assertEquals("query.anim_time", call.options().get("function").getAsString());
        assertEquals(0, call.options().get("arg_count").getAsInt());
    }

    @Test
    void mathCall() {
        MolangDecompiler.ExprFragment f = MolangDecompiler.decompileExpression("math.sin(query.anim_time)");
        NodeInstance call = firstByType(f.nodes(), "math.call");
        assertEquals("math.sin", call.options().get("function").getAsString());
        assertEquals(1, call.options().get("arg_count").getAsInt());
        assertEquals(firstByType(f.nodes(), "query.call").uid(), wireSource(f.wires(), call.uid(), "arg1"));
    }

    // ---------- 资源引用（RC 表达式槽） ----------

    @Test
    void resourceRefs() {
        MolangDecompiler.ExprFragment g = MolangDecompiler.decompileExpression("geometry.default");
        NodeInstance geo = firstByType(g.nodes(), "ref.geometry");
        assertEquals("default", geo.options().get("short_name").getAsString());
        assertEquals(new PortRef(geo.uid(), "ref"), g.output().orElseThrow());

        assertEquals("a", firstByType(MolangDecompiler.decompileExpression("texture.a")
                .nodes(), "ref.texture").options().get("short_name").getAsString());
        assertEquals("entity_alphatest", firstByType(MolangDecompiler.decompileExpression("material.entity_alphatest")
                .nodes(), "ref.material").options().get("short_name").getAsString());
    }

    // ---------- 语句：条件赋值脱糖 ----------

    @Test
    void conditionalAssignmentNoElse() {
        MolangDecompiler.ExecFragment f = MolangDecompiler.decompileStatements("v.a?{v.x=1;v.y=2;}");
        assertFalse(hasCode(f.diagnostics(), DecompileDiagnostics.UNSUPPORTED_IMPORT));
        var setVars = allByType(f.nodes(), "exec.set_var");
        assertEquals(2, setVars.size());
        assertEquals(2, f.chain().size());
        var ternaries = allByType(f.nodes(), "op.ternary");
        assertEquals(2, ternaries.size());
        // 第一变量 v.x：a=const.int 1，b=var.get 自引用，cond=var.get v.a
        NodeInstance ternary = ternaries.get(0);
        NodeInstance condNode = nodeByUid(f, wireSource(f.wires(), ternary.uid(), "cond"));
        assertEquals("var.get", condNode.type());
        assertEquals("variable.a", condNode.options().get("name").getAsString());
        NodeInstance aNode = nodeByUid(f, wireSource(f.wires(), ternary.uid(), "a"));
        assertEquals("const.int", aNode.type());
        NodeInstance bNode = nodeByUid(f, wireSource(f.wires(), ternary.uid(), "b"));
        assertEquals("var.get", bNode.type());
        assertEquals("variable.x", bNode.options().get("name").getAsString());
        // set_var：ternary.out → value，name 带根全名
        NodeInstance setX = setVars.get(0);
        assertEquals("variable.x", setX.options().get("name").getAsString());
        assertEquals(ternary.uid(), wireSource(f.wires(), setX.uid(), "value"));
    }

    @Test
    void conditionalAssignmentWithElse() {
        MolangDecompiler.ExecFragment f = MolangDecompiler.decompileStatements("v.a?{v.x=1;}:{v.x=2;}");
        assertFalse(hasCode(f.diagnostics(), DecompileDiagnostics.UNSUPPORTED_IMPORT));
        NodeInstance ternary = firstByType(f.nodes(), "op.ternary");
        assertEquals("const.int", nodeByUid(f, wireSource(f.wires(), ternary.uid(), "a")).type());
        NodeInstance bNode = nodeByUid(f, wireSource(f.wires(), ternary.uid(), "b"));
        assertEquals("const.int", bNode.type());
        assertEquals(2, bNode.options().get("value").getAsInt());
    }

    @Test
    void conditionalAssignmentTempSelfRef() {
        MolangDecompiler.ExecFragment f = MolangDecompiler.decompileStatements("v.a?{temp.t=1;}");
        NodeInstance ternary = firstByType(f.nodes(), "op.ternary");
        NodeInstance bNode = nodeByUid(f, wireSource(f.wires(), ternary.uid(), "b"));
        assertEquals("temp.get", bNode.type());
        assertEquals("temp.t", bNode.options().get("name").getAsString());
    }

    @Test
    void conditionalAssignmentNested() {
        // v.x = v.a ? (v.b ? 2 : 1) : v.x（顺序语义：先 v=1，再 v.b 条件覆盖）
        MolangDecompiler.ExecFragment f = MolangDecompiler.decompileStatements("v.a?{v.x=1; v.b?{v.x=2;}}");
        assertFalse(hasCode(f.diagnostics(), DecompileDiagnostics.UNSUPPORTED_IMPORT));
        var ternaries = allByType(f.nodes(), "op.ternary");
        assertEquals(2, ternaries.size());
        // 外层 cond = var.get v.a
        NodeInstance outer = ternaries.stream()
                .filter(t -> {
                    String condUid = wireSource(f.wires(), t.uid(), "cond");
                    NodeInstance condNode = nodeByUid(f, condUid);
                    return "var.get".equals(condNode.type())
                            && "variable.a".equals(condNode.options().get("name").getAsString());
                }).findFirst().orElseThrow();
        // 外层 a = 内层三元（cond = var.get v.b）
        NodeInstance inner = nodeByUid(f, wireSource(f.wires(), outer.uid(), "a"));
        assertEquals("op.ternary", inner.type());
        NodeInstance innerCond = nodeByUid(f, wireSource(f.wires(), inner.uid(), "cond"));
        assertEquals("variable.b", innerCond.options().get("name").getAsString());
        // 内层 a=2，b=1（顺序覆盖）；外层 b = var.get 自引用
        assertEquals(2, nodeByUid(f, wireSource(f.wires(), inner.uid(), "a")).options().get("value").getAsInt());
        assertEquals(1, nodeByUid(f, wireSource(f.wires(), inner.uid(), "b")).options().get("value").getAsInt());
        NodeInstance outerB = nodeByUid(f, wireSource(f.wires(), outer.uid(), "b"));
        assertEquals("var.get", outerB.type());
        assertEquals("variable.x", outerB.options().get("name").getAsString());
    }

    @Test
    void conditionalNonAssignmentStillUnsupported() {
        MolangDecompiler.ExecFragment f = MolangDecompiler.decompileStatements("v.a?{query.foo();}");
        assertTrue(hasCode(f.diagnostics(), DecompileDiagnostics.UNSUPPORTED_IMPORT));
        // 不产生条件赋值节点
        assertTrue(allByType(f.nodes(), "op.ternary").isEmpty());
    }

    private static NodeInstance nodeByUid(MolangDecompiler.ExecFragment f, String uid) {
        return f.nodes().stream().filter(n -> n.uid().equals(uid)).findFirst().orElseThrow();
    }

    // ---------- 语句：赋值 / loop / for_each / 控制流 ----------

    @Test
    void assignment() {
        MolangDecompiler.ExecFragment f = MolangDecompiler.decompileStatements("variable.x = 1;");
        assertEquals(1, f.chain().size());
        NodeInstance setVar = firstByType(f.nodes(), "exec.set_var");
        assertEquals("variable", setVar.options().get("root").getAsString());
        assertEquals("variable.x", setVar.options().get("name").getAsString());
        assertEquals(firstByType(f.nodes(), "const.int").uid(), wireSource(f.wires(), setVar.uid(), "value"));
    }

    @Test
    void assignmentToTemp() {
        MolangDecompiler.ExecFragment f = MolangDecompiler.decompileStatements("t.x = 1;");
        NodeInstance setVar = firstByType(f.nodes(), "exec.set_var");
        assertEquals("temp", setVar.options().get("root").getAsString());
        assertEquals("temp.x", setVar.options().get("name").getAsString());
    }

    @Test
    void loopStatement() {
        MolangDecompiler.ExecFragment f =
                MolangDecompiler.decompileStatements("loop(3, { variable.x = variable.x + 1; });");
        NodeInstance loop = firstByType(f.nodes(), "exec.loop");
        assertEquals(List.of(loop.uid()), f.chain());
        assertEquals(firstByType(f.nodes(), "const.int").uid(), wireSource(f.wires(), loop.uid(), "count"));
        // body：链尾 exec_out → loop.body
        NodeInstance setVar = firstByType(f.nodes(), "exec.set_var");
        assertEquals(setVar.uid(), wireSource(f.wires(), loop.uid(), "body"));
        assertTrue(wireInto(f.wires(), setVar.uid(), "value").isPresent());
    }

    @Test
    void forEachStatement() {
        MolangDecompiler.ExecFragment f =
                MolangDecompiler.decompileStatements("for_each(t.item, variable.arr, { temp.y = 2; });");
        NodeInstance forEach = firstByType(f.nodes(), "exec.for_each");
        assertEquals("temp.item", forEach.options().get("var_name").getAsString());
        assertEquals(firstByType(f.nodes(), "var.get").uid(), wireSource(f.wires(), forEach.uid(), "array"));
        assertEquals(firstByType(f.nodes(), "exec.set_var").uid(), wireSource(f.wires(), forEach.uid(), "body"));
    }

    @Test
    void controlFlowStatements() {
        assertEquals(1, allByType(MolangDecompiler.decompileStatements("break;").nodes(), "exec.break").size());
        assertEquals(1, allByType(MolangDecompiler.decompileStatements("continue;").nodes(), "exec.continue").size());

        MolangDecompiler.ExecFragment ret = MolangDecompiler.decompileStatements("return 1;");
        NodeInstance returnNode = firstByType(ret.nodes(), "exec.return");
        assertEquals(firstByType(ret.nodes(), "const.int").uid(), wireSource(ret.wires(), returnNode.uid(), "value"));
    }

    @Test
    void queryCallStatement() {
        MolangDecompiler.ExecFragment f = MolangDecompiler.decompileStatements("query.reset();");
        NodeInstance call = firstByType(f.nodes(), "exec.call");
        assertEquals("query.reset", call.options().get("function").getAsString());
        assertEquals(List.of(call.uid()), f.chain());
    }

    // ---------- exec 链（反向汇入槽模型） ----------

    @Test
    void statementChainWireDirection() {
        MolangDecompiler.ExecFragment f =
                MolangDecompiler.decompileStatements("variable.a = 1; variable.b = 2; variable.c = 3;");
        assertEquals(3, f.chain().size());
        String head = f.chain().get(0);
        String mid = f.chain().get(1);
        String tail = f.chain().get(2);
        // 前驱 exec_out → 后继 exec_in；槽口连链尾（tail().port = exec_out）
        assertEquals(head, wireSource(f.wires(), mid, "exec_in"));
        assertEquals(mid, wireSource(f.wires(), tail, "exec_in"));
        assertTrue(wireInto(f.wires(), head, "exec_in").isEmpty());
        assertEquals(new PortRef(tail, "exec_out"), f.tail().orElseThrow());
    }

    // ---------- 不支持结构 ----------

    @Test
    void unsupportedIndex() {
        MolangDecompiler.ExprFragment f = MolangDecompiler.decompileExpression("1 + variable.a[2]");
        assertEquals(1, countCode(f.diagnostics(), DecompileDiagnostics.UNSUPPORTED_IMPORT));
        // 便签留原文
        assertTrue(f.stickyNotes().stream().anyMatch(s -> s.text().contains("variable.a[2]")));
        // const.number 0 占位接入 op.binary.b，a 侧正常
        NodeInstance op = firstByType(f.nodes(), "op.binary");
        NodeInstance placeholder = firstByType(f.nodes(), "const.number");
        assertEquals(0.0, placeholder.options().get("value").getAsDouble());
        assertEquals(placeholder.uid(), wireSource(f.wires(), op.uid(), "b"));
        assertEquals(firstByType(f.nodes(), "const.int").uid(), wireSource(f.wires(), op.uid(), "a"));
    }

    @Test
    void unsupportedArrow() {
        MolangDecompiler.ExprFragment f = MolangDecompiler.decompileExpression("variable.a->query.b");
        assertTrue(hasCode(f.diagnostics(), DecompileDiagnostics.UNSUPPORTED_IMPORT));
        assertTrue(f.stickyNotes().stream().anyMatch(s -> s.text().contains("->")));
        // const 0 占位为产物输出
        assertEquals(firstByType(f.nodes(), "const.number").uid(), f.output().orElseThrow().node());
    }

    @Test
    void unsupportedMemberOnCallResult() {
        MolangDecompiler.ExprFragment f = MolangDecompiler.decompileExpression("query.foo().bar");
        assertTrue(hasCode(f.diagnostics(), DecompileDiagnostics.UNSUPPORTED_IMPORT));
        assertTrue(f.stickyNotes().stream().anyMatch(s -> s.text().contains("query.foo().bar")));
    }

    @Test
    void unsupportedStatementSkippedButChainContinues() {
        // 非法赋值目标语句 → 诊断 + 便签跳过；其余语句照常入链
        MolangDecompiler.ExecFragment f =
                MolangDecompiler.decompileStatements("variable.a = 1; context.x = 2; variable.b = 3;");
        assertTrue(hasCode(f.diagnostics(), DecompileDiagnostics.UNSUPPORTED_IMPORT));
        assertTrue(f.stickyNotes().stream().anyMatch(s -> s.text().contains("context.x = 2")));
        assertEquals(2, f.chain().size());
        assertEquals(2, allByType(f.nodes(), "exec.set_var").size());
    }

    @Test
    void parseFailure() {
        MolangDecompiler.ExprFragment f = MolangDecompiler.decompileExpression("1 +* 2");
        assertTrue(hasCode(f.diagnostics(), DecompileDiagnostics.PARSE_FAILURE));
        assertTrue(f.stickyNotes().stream().anyMatch(s -> s.text().equals("1 +* 2")));
        // const 0 占位
        assertEquals(firstByType(f.nodes(), "const.number").uid(), f.output().orElseThrow().node());
    }

    @Test
    void breakWithValue() {
        MolangDecompiler.ExecFragment f = MolangDecompiler.decompileStatements("loop(2, { break 5; });");
        assertTrue(hasCode(f.diagnostics(), DecompileDiagnostics.UNSUPPORTED_IMPORT));
        // break 节点仍入 body 链（值语义丢失，便签留原文）
        NodeInstance loop = firstByType(f.nodes(), "exec.loop");
        assertEquals(firstByType(f.nodes(), "exec.break").uid(), wireSource(f.wires(), loop.uid(), "body"));
    }
}
