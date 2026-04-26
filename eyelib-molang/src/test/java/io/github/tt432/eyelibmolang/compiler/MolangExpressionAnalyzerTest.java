package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MolangExpressionAnalyzerTest {
    @AfterEach
    void tearDown() {
        MolangMappingTree.INSTANCE.clear();
    }

    @Test
    void arithmeticExpressionsAreCompileTimeEvaluableAndEnumerable() {
        MolangExpressionAnalysis analysis = MolangExpressionAnalyzer.analyze("1 + 2 * 3");

        assertTrue(analysis.compileTimeEvaluable());
        assertTrue(analysis.runtimeEnumerable());
        assertTrue(analysis.sideEffectFree());
    }

    @Test
    void randomFunctionIsNeitherCompileTimeEvaluableNorEnumerable() {
        MolangExpressionAnalysis analysis = MolangExpressionAnalyzer.analyze("math.random(0, 1)");

        assertFalse(analysis.compileTimeEvaluable());
        assertFalse(analysis.runtimeEnumerable());
    }

    @Test
    void configuredRuntimeVariableStaysEnumerableButNotCompileTimeEvaluable() {
        MolangExpressionAnalysis analysis = MolangExpressionAnalyzer.analyze(
                "variable.fps",
                MolangExpressionEnvironment.builder().runtimeEnumerableVariable("variable.fps").build()
        );

        assertFalse(analysis.compileTimeEvaluable());
        assertTrue(analysis.runtimeEnumerable());
        assertTrue(analysis.sideEffectFree());
    }

    @Test
    void thisIsNotCompileTimeEvaluableAndReportsThisNotFoldable() {
        MolangExpressionAnalysis analysis = MolangExpressionAnalyzer.analyze("this");

        assertFalse(analysis.compileTimeEvaluable());
        assertTrue(analysis.blockers().contains("this_not_foldable"));
    }
}
