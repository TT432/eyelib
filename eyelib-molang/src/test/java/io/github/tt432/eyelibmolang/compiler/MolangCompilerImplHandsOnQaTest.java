package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.compiler.binding.BindResult;
import io.github.tt432.eyelibmolang.compiler.binding.MolangBinder;
import io.github.tt432.eyelibmolang.compiler.frontend.HandwrittenMolangAstParserFrontend;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MolangCompilerImplHandsOnQaTest {

    @Test
    void compileAndEvaluateSimpleAdditionReturnsThreePointZero() {
        MolangCompilerImpl compiler = new MolangCompilerImpl();

        CompiledMolangExpression compiled = compiler.compile("1+2", CompileContext.defaults());
        float value = compiled.evaluate(new MolangScope()).asFloat();

        assertEquals(3.0F, value, 0.0001F);
    }

    @Test
    void compileUnknownFunctionThrowsExpressionCompileExceptionWithMessage() {
        MolangCompilerImpl compiler = new MolangCompilerImpl();

        ExpressionCompileException exception = assertThrows(
                ExpressionCompileException.class,
                () -> compiler.compile("1+nonexistent()", CompileContext.defaults())
        );

        assertNotNull(exception.getMessage());
        assertFalse(exception.getMessage().isBlank());
    }

    @Test
    void bytecodeEmitterOutputStartsWithCafeBabeMagic() {
        MolangAst.ExprSet ast = HandwrittenMolangAstParserFrontend.INSTANCE
                .parseExprSetAst("1+2")
                .orElseThrow(() -> new AssertionError("Expected parser to accept expression 1+2"));

        BindResult bindResult = new MolangBinder().bind(ast);
        assertTrue(bindResult.diagnostics().isEmpty(), "Expected bind diagnostics to be empty for 1+2");

        byte[] classBytes = MolangBytecodeEmitter.emit(new BoundMolangCompilerInput(
                "1+2",
                bindResult.root(),
                CompileContext.defaults()
        ));

        assertTrue(classBytes.length >= 4, "Class file bytes must contain a magic header");

        int magic = ((classBytes[0] & 0xFF) << 24)
                | ((classBytes[1] & 0xFF) << 16)
                | ((classBytes[2] & 0xFF) << 8)
                | (classBytes[3] & 0xFF);
        assertEquals(0xCAFEBABE, magic);
    }
}
