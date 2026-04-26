package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class MolangValueConstantFoldingTest {
    @AfterEach
    void tearDown() {
        MolangMappingTree.INSTANCE.clear();
    }

    @Test
    void arithmeticExpressionFoldsIntoConstFunction() {
        MolangValue value = new MolangValue("1 + 2");

        assertInstanceOf(MolangValue.ConstMolangFunction.class, value.method());
        assertEquals(3F, value.eval(new MolangScope()), 0.0001F);
    }

    @Test
    void deterministicMathFunctionFoldsIntoConstFunction() {
        MolangValue value = new MolangValue("math.sin(30)");

        assertInstanceOf(MolangValue.ConstMolangFunction.class, value.method());
        assertEquals(0.5F, value.eval(new MolangScope()), 0.0001F);
    }

    @Test
    void randomFunctionDoesNotFoldIntoConstFunction() {
        MolangValue value = new MolangValue("math.random(0, 1)");

        assertFalse(value.method() instanceof MolangValue.ConstMolangFunction);
    }

    @Test
    void thisUsesDeferredPhaseFiveCompatibilityFallbackToZeroInCurrentCompilePath() {
        MolangValue value = new MolangValue("this");

        assertFalse(value.method() instanceof MolangValue.ConstMolangFunction);
        assertEquals(0F, value.eval(new MolangScope()), 0.0001F);
    }
}
