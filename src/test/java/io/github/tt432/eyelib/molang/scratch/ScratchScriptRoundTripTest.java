package io.github.tt432.eyelib.molang.scratch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ScratchScript} 双向转换：fromMolang → toMolang 的文本口径、
 * 导入结构、非法字段的显式报错。
 *
 * @author TT432
 */
class ScratchScriptRoundTripTest {

    private static String roundTrip(String source) {
        return ScratchScript.fromMolang(source).orElseThrow(() -> new AssertionError("解析失败: " + source))
                .toMolang();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "0",
            "query.anim_time",
            "math.clamp(query.anim_time, 0, 1) * 360",
            "variable.x = 1 + math.sin(query.anim_time * 2)",
            "query.is_baby ? 0.5 : 1.0",
            "query.is_baby ? 0.5",
            "a ?? b ?? c",
            "variable.a->context.other.b",
            "'hello'",
            "this",
            "loop(10, { temp.i = temp.i + 1; })",
            "for_each(v.b, q.bs, { v.x = 1; })",
            "{ v.x = 1; v.y = 2; }",
    })
    void roundTripsSingleExpressionsAndStatements(String source) {
        assertEquals(source, roundTrip(source));
    }

    @Test
    void roundTripsMultiStatement() {
        assertEquals("v.x = 1;\nv.y = 2;", roundTrip("v.x = 1; v.y = 2;"));
        assertEquals("v.x = 1;\nreturn v.x * 2;", roundTrip("v.x = 1; return v.x * 2;"));
    }

    @Test
    void emptyScriptEmitsEmpty() {
        assertEquals("", ScratchScript.empty().toMolang());
        // 空文本按解析器口径为字面量 0（molang 空表达式语义）
        assertEquals("0", ScratchScript.fromMolang("").orElseThrow().toMolang());
    }

    @Test
    void importsCallWithTypedChildren() {
        ScratchScript script = ScratchScript.fromMolang("math.clamp(q.a, 0, 1)").orElseThrow();
        assertEquals(1, script.statements().size());
        ScratchBlock stmt = script.statements().get(0);
        assertEquals(ScratchKind.STMT_EXPR, stmt.kind());
        ScratchBlock call = stmt.socket("expr").child();
        assertEquals(ScratchKind.CALL, call.kind());
        assertEquals("math.clamp", call.field("name"));
        assertEquals(3, call.sockets().size());
        assertEquals(ScratchKind.VAR, call.socket(0).child().kind());
        assertEquals("q.a", call.socket(0).child().field("path"));
        assertEquals(ScratchKind.NUM, call.socket(1).child().kind());
        assertEquals(ScratchCategory.MATH, ScratchKind.categoryOf(call));
    }

    @Test
    void importsLoopWithBody() {
        ScratchScript script = ScratchScript.fromMolang("loop(5, { temp.i = temp.i + 1; break; })")
                .orElseThrow();
        ScratchBlock loop = script.statements().get(0);
        assertEquals(ScratchKind.CTRL_LOOP, loop.kind());
        assertEquals(2, loop.body().size());
        assertEquals(ScratchKind.STMT_ASSIGN, loop.body().get(0).kind());
        assertEquals(ScratchKind.STMT_BREAK, loop.body().get(1).kind());
    }

    @Test
    void boolShapeForPredicates() {
        ScratchBlock cmp = ScratchScript.fromMolang("a < b").orElseThrow()
                .statements().get(0).socket("expr").child();
        assertEquals(ScratchShape.BOOL, ScratchKind.shapeOf(cmp));
        ScratchBlock add = ScratchScript.fromMolang("a + b").orElseThrow()
                .statements().get(0).socket("expr").child();
        assertEquals(ScratchShape.PILL, ScratchKind.shapeOf(add));
        ScratchBlock not = ScratchScript.fromMolang("!q.a").orElseThrow()
                .statements().get(0).socket("expr").child();
        assertEquals(ScratchShape.BOOL, ScratchKind.shapeOf(not));
    }

    @Test
    void fallsBackToRawForExoticExpressions() {
        // callee 非点链：积木体系无对应形状 → RAW 原文直通
        ScratchScript script = ScratchScript.fromMolang("(v.a)(1)").orElseThrow();
        ScratchBlock raw = script.statements().get(0).socket("expr").child();
        assertEquals(ScratchKind.RAW, raw.kind());
        assertEquals("(v.a)(1)", raw.field("text"));
        assertEquals("(v.a)(1)", script.toMolang());
    }

    @Test
    void exportRejectsIllegalFields() {
        ScratchBlock badNumber = ScratchBlock.of(ScratchKind.NUM);
        badNumber.setField("text", "abc");
        ScratchBlock stmt = ScratchBlock.of(ScratchKind.STMT_EXPR);
        stmt.socket("expr").setChild(badNumber);
        ScratchScript script = ScratchScript.empty();
        script.statements().add(stmt);
        assertThrows(ScratchExport.ExportException.class, script::toMolang);

        ScratchBlock badPath = ScratchBlock.of(ScratchKind.VAR);
        badPath.setField("path", "1x");
        stmt.socket("expr").setChild(badPath);
        assertThrows(ScratchExport.ExportException.class, script::toMolang);

        ScratchBlock badString = ScratchBlock.of(ScratchKind.STR);
        badString.setField("text", "it's");
        stmt.socket("expr").setChild(badString);
        assertThrows(ScratchExport.ExportException.class, script::toMolang);
    }

    @Test
    void copyIsDeep() {
        ScratchScript script = ScratchScript.fromMolang("math.max(v.a, 1)").orElseThrow();
        ScratchBlock original = script.statements().get(0);
        ScratchBlock copy = original.copy();
        copy.socket("expr").child().socket(0).child().setField("path", "v.b");
        assertEquals("math.max(v.a, 1)", script.toMolang());
        assertTrue(copy != original);
    }
}
