package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.type.MolangFloat;
import io.github.tt432.eyelibmolang.type.MolangObject;
import io.github.tt432.eyelibmolang.type.MolangString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TT432
 */
class MolangTernaryConditionalTest {

    private final MolangCompilerImpl compiler = new MolangCompilerImpl();
    private final MolangScope scope = new MolangScope();

    @Test
    void conditionTruePicksWhenTrueBranch() {
        scope.set("variable.flag", MolangFloat.ONE);
        CompiledMolangExpression expr = compiler.compile("v.flag?100:200", CompileContext.defaults());
        MolangObject result = expr.evaluate(scope);
        assertEquals(100.0F, result.asFloat(), 0.0001F);
    }

    @Test
    void conditionFalsePicksWhenFalseBranch() {
        scope.set("variable.flag", MolangFloat.ZERO);
        CompiledMolangExpression expr = compiler.compile("v.flag?100:200", CompileContext.defaults());
        MolangObject result = expr.evaluate(scope);
        assertEquals(200.0F, result.asFloat(), 0.0001F);
    }

    @Test
    void undefinedVariableAsConditionFallsToElseBranch() {
        CompiledMolangExpression expr = compiler.compile("v.undefined?100:200", CompileContext.defaults());
        MolangObject result = expr.evaluate(scope);
        assertEquals(200.0F, result.asFloat(), 0.0001F);
    }

    @Test
    void undefinedVariableAndOpUndefinedVariableFallsToElseBranch() {
        CompiledMolangExpression expr = compiler.compile("(v.a&&v.b)?100:200", CompileContext.defaults());
        MolangObject result = expr.evaluate(scope);
        assertEquals(200.0F, result.asFloat(), 0.0001F);
    }

    @Test
    void stringThenBranchWithFalseConditionReturnsElseBranch() {
        scope.set("variable.test.string", new MolangString("hello"));
        CompiledMolangExpression expr = compiler.compile("0?v.test.string:'world'", CompileContext.defaults());
        MolangObject result = expr.evaluate(scope);
        assertEquals("world", result.asString());
    }

    @Test
    void nonNullResultIsNotMolangNull() {
        CompiledMolangExpression expr = compiler.compile("1?42:99", CompileContext.defaults());
        MolangObject result = expr.evaluate(scope);
        assertEquals(42.0F, result.asFloat(), 0.0001F);
    }

    @Test
    void nestedTernaryWorksCorrectly() {
        scope.set("variable.a", MolangFloat.ONE);
        scope.set("variable.b", MolangFloat.ZERO);
        CompiledMolangExpression expr = compiler.compile("v.a?v.b?10:20:30", CompileContext.defaults());
        MolangObject result = expr.evaluate(scope);
        assertEquals(20.0F, result.asFloat(), 0.0001F);
    }

    @Test
    void renderControllerGeometryPatternFallsToElseWithUndefinedVars() {
        scope.set("geometry.default", new MolangString("geometry.test_model"));
        CompiledMolangExpression expr = compiler.compile(
                "(v.cdrzno&&v.aybhly)?Geometry.smithw:Geometry.default", CompileContext.defaults());
        MolangObject result = expr.evaluate(scope);
        assertEquals("geometry.test_model", result.asString());
    }
}
