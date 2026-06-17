package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.type.MolangNull;
import io.github.tt432.eyelibmolang.type.MolangObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class MolangControlFlowPipelineTest {
    private final MolangCompilerImpl compiler = new MolangCompilerImpl();

    @Test
    void loopNormalCompletionReturnsNull() {
        assertSame(MolangNull.INSTANCE, eval("loop(3, {})"));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "loop(10, { break 42.0 })~42.0",
            "t.n = 3; loop(t.n, { t.x = t.x + 1 }); t.x~3.0",
            "t.x = 0; loop(5, { continue; t.x = t.x + 1 }); t.x~0.0",
            "t.x = 0; loop(5, { continue (t.x = t.x + 1) }); t.x~5.0",
            "t.i = 0; loop(10, { t.i = t.i + 1; t.i > 3 ? { break t.i } : 0 }); t.i~4.0",
            "t.x = 0; loop(3, { loop(3, { break }); t.x = t.x + 1 }); t.x~3.0",
            "loop(3, { loop(3, {}); break 7.0 })~7.0"
    }, delimiter = '~')
    void controlFlowExpressionsEvaluate(String expression, float expected) {
        assertEquals(expected, eval(expression).asFloat(), 0.0001);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "loop(10, { break })",
            "loop(0, { break 1.0 })",
            "loop(-5, { break 1.0 })",
            "for_each(v, 42.0, {})"
    })
    void controlFlowExpressionsReturnNull(String expression) {
        assertSame(MolangNull.INSTANCE, eval(expression));
    }

    @Test
    void binaryConditionalBreakExpressionStopsLoop() {
        String expression = "v.x = 0; v.y = 1; loop(10, {t.x = v.x + v.y; v.x = v.y; v.y = t.x; (v.y > 20) ? break;}); v.y";

        assertEquals(21.0F, eval(expression).asFloat(), 0.0001);
    }

    @Test
    void binaryConditionalContinueExpressionSkipsRemainingLoopBody() {
        String expression = "v.x = 0; loop(10, {(v.x > 5) ? continue; v.x = v.x + 1;}); v.x";

        assertEquals(6.0F, eval(expression).asFloat(), 0.0001);
    }

    @Test
    void breakExpressionOutsideLoopReportsBindError() {
        ExpressionCompileException exception = assertThrows(
                ExpressionCompileException.class,
                () -> compiler.compile("1 ? break", CompileContext.defaults())
        );

        assertTrue(exception.diagnostics().stream().anyMatch(diagnostic -> diagnostic.contains("break outside of loop")));
    }

    private MolangObject eval(String expression) {
        CompiledMolangExpression compiled = compiler.compile(expression, CompileContext.defaults());
        return compiled.evaluate(new MolangScope());
    }
}
