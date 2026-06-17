package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.compiler.binding.BindResult;
import io.github.tt432.eyelib.molang.compiler.binding.MolangBinder;
import io.github.tt432.eyelib.molang.compiler.frontend.HandwrittenMolangAstParserFrontend;
import io.github.tt432.eyelib.molang.compiler.frontend.ast.MolangAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class MolangCompilerImplHandsOnQaTest {

    @Test
    void compileAndEvaluateSimpleAdditionReturnsThreePointZero() {
        MolangCompilerImpl compiler = new MolangCompilerImpl();

        CompiledMolangExpression compiled = compiler.compile("1+2", CompileContext.defaults());
        float value = compiled.evaluate(new MolangScope()).asFloat();

        assertEquals(3.0F, value, 0.0001F);
    }

    @Test
    void compileUnknownFunctionCompilesSuccessfully() {
        // 完整的字节码发射器覆盖后，所有表达式都能成功编译。
        // 未知函数在运行时通过 MolangRuntimeSupport 解析为 MolangNull。
        MolangCompilerImpl compiler = new MolangCompilerImpl();
        CompiledMolangExpression compiled = compiler.compile("1+nonexistent()", CompileContext.defaults());
        assertNotNull(compiled);
        assertNotNull(compiled.evaluate(new MolangScope()));
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