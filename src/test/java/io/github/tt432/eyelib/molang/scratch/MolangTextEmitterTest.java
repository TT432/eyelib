package io.github.tt432.eyelib.molang.scratch;

import io.github.tt432.eyelib.molang.compiler.frontend.HandwrittenMolangAstParserFrontend;
import io.github.tt432.eyelib.molang.compiler.frontend.ast.MolangAst;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link MolangTextEmitter} 优先级与括号正确性：parse → emit 的文本必须
 * 按最小括号口径还原，且 emit → parse → emit 稳定（幂等）。
 *
 * @author TT432
 */
class MolangTextEmitterTest {

    private static String emit(String source) {
        MolangAst.ExprSet ast = new HandwrittenMolangAstParserFrontend().parseExprSetAst(source)
                .orElseThrow(() -> new AssertionError("语料解析失败: " + source));
        return MolangTextEmitter.emitScript(ast);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            // 优先级天然正确，不加括号
            "1 + 2 * 3",
            "1 * 2 + 3",
            "a && b || c",
            "a || b && c",
            "a == b && c < d",
            "a ?? b ?? c",
            "a - b - c",
            "query.a ? 1 : 2",
            "query.a ?? 0 ? 1 : query.b",
            "math.clamp(query.anim_time, 0, 1) * 360",
            "query.is_name_any('a', 'b')",
            "variable.a->context.other.b",
            "v.arr[math.floor(q.anim_time)]",
            "-x.y",
            "!q.a",
            "a = b = c",
    })
    void emitsWithoutRedundantParens(String source) {
        assertEquals(source, emit(source));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            // 括号是语义必需，发射器必须补回（GroupingExpr 不保留时同形）
            "(1 + 2) * 3",
            "1 * (2 + 3)",
            "a - (b - c)",
            "a ?? (b ?? c)",
            "a && (b || c)",
            "-(a + b)",
            "!(a == b)",
            "(a + b).c",
            "(a ? b : c) ? d : e",
    })
    void emitsRequiredParens(String source) {
        assertEquals(source, emit(source));
    }

    @Test
    void emitsLoopAndForEach() {
        assertEquals("loop(10, { temp.i = temp.i + 1; })", emit("loop(10, { temp.i = temp.i + 1; });"));
        assertEquals("for_each(v.b, q.bs, { v.x = 1; })", emit("for_each(v.b, q.bs, { v.x = 1; })"));
    }

    @Test
    void emitsMultiStatementScript() {
        assertEquals("v.x = 1;\nv.y = 2;", emit("v.x = 1; v.y = 2;"));
        assertEquals("v.x = 1;\nreturn v.x * 2;", emit("v.x = 1; return v.x * 2;"));
    }

    @Test
    void emitsBreakContinueValues() {
        assertEquals("loop(10, { break 5; })", emit("loop(10, { break 5; });"));
        assertEquals("loop(10, { continue; })", emit("loop(10, { continue; });"));
    }

    @ParameterizedTest(name = "幂等 [{index}] {0}")
    @ValueSource(strings = {
            "1 + 2 * 3",
            "(1 + 2) * 3",
            "a ?? (b ?? c)",
            "query.a ? 1 : 2",
            "v.x = 1;\nreturn v.x * 2;",
            "loop(10, { temp.i = temp.i + 1; break; })",
    })
    void emitIsIdempotent(String source) {
        String once = emit(source);
        assertEquals(once, emit(once));
    }
}
