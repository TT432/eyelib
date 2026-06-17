package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.type.MolangObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class MolangFullPipelineTest {
    private final MolangCompilerImpl compiler = new MolangCompilerImpl();

    @ParameterizedTest
    @CsvSource(value = {
            "1+2~3.0",
            "5-3~2.0",
            "4*3~12.0",
            "8/2~4.0",
            "5/0~0.0"
    }, delimiter = '~')
    void binaryArithmetic(String expression, float expected) {
        assertEvaluatesTo(expression, expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "1<2~1.0",
            "2<1~0.0",
            "2<=2~1.0",
            "3>=3~1.0",
            "3==3~1.0",
            "3!=3~0.0"
    }, delimiter = '~')
    void comparisonOperators(String expression, float expected) {
        assertEvaluatesTo(expression, expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "1&&2~1.0",
            "0&&2~0.0",
            "1||0~1.0",
            "0||0~0.0"
    }, delimiter = '~')
    void logicalOperators(String expression, float expected) {
        assertEvaluatesTo(expression, expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "1??2~1.0",
            "does_not_exist??42~42.0"
    }, delimiter = '~')
    void nullCoalesce(String expression, float expected) {
        assertEvaluatesTo(expression, expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "'hello'",
            "''"
    }, quoteCharacter = '"')
    void stringLiterals(String expression) {
        CompiledMolangExpression compiled = compiler.compile(expression, CompileContext.defaults());
        MolangObject value = compiled.evaluate(new MolangScope());

        assertNotNull(value);
    }

    @ParameterizedTest
    @CsvSource(value = "this~0.0", delimiter = '~')
    void thisExpression(String expression, float expected) {
        assertEvaluatesTo(expression, expected);
    }

    @ParameterizedTest
    @CsvSource(value = "{ t.a = 1; return t.a + 2; }~3.0", delimiter = '~')
    void returnInBlock(String expression, float expected) {
        assertEvaluatesTo(expression, expected);
    }

    private void assertEvaluatesTo(String expression, float expected) {
        CompiledMolangExpression compiled = compiler.compile(expression, CompileContext.defaults());
        MolangObject value = compiled.evaluate(new MolangScope());

        assertNotNull(value);
        assertTrue(Float.isFinite(value.asFloat()));
        assertEquals(expected, value.asFloat(), 0.0001);
    }
}