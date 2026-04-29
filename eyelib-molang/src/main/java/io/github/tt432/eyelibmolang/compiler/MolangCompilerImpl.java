package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.compiler.binding.BindDiagnostic;
import io.github.tt432.eyelibmolang.compiler.binding.BindResult;
import io.github.tt432.eyelibmolang.compiler.binding.MolangBinder;
import io.github.tt432.eyelibmolang.compiler.frontend.HandwrittenMolangAstParserFrontend;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;

import java.lang.reflect.Constructor;
import java.util.List;

public final class MolangCompilerImpl implements MolangCompiler {
    private static final MolangBinder BINDER = new MolangBinder();

    @Override
    public CompiledMolangExpression compile(String expression, CompileContext ctx) {
        CompileContext effectiveCtx = ctx == null ? CompileContext.defaults() : ctx;
        try {
            // Step 1: Parse source into AST
            MolangAst.ExprSet ast = HandwrittenMolangAstParserFrontend.INSTANCE
                    .parseExprSetAst(expression)
                    .orElseThrow(() ->
                            new ExpressionCompileException(expression,
                                    "Failed to parse molang expression: syntax error."));

            // Step 2: Bind AST — resolve identifiers, validate semantics
            BindResult bindResult = BINDER.bind(ast, effectiveCtx.diagnosticsMode());
            if (bindResult.hasErrors()) {
                List<String> diagnostics = bindResult.diagnostics().stream()
                        .filter(d -> d.severity() == BindDiagnostic.Severity.ERROR)
                        .map(BindDiagnostic::message)
                        .toList();
                throw new ExpressionCompileException(expression,
                        "Failed to bind molang expression: semantic errors detected.", diagnostics);
            }

            // Step 3: Emit JVM bytecode from bound AST
            BoundMolangCompilerInput input = new BoundMolangCompilerInput(
                    expression, bindResult.root(), effectiveCtx);
            byte[] classBytes = MolangBytecodeEmitter.emit(input);

            // Step 4: Load generated class and wrap as CompiledMolangExpression
            return instantiate(classBytes);
        } catch (ExpressionCompileException e) {
            throw e;
        } catch (Throwable t) {
            throw new ExpressionCompileException(expression,
                    "Failed to compile molang expression: unexpected error.", t);
        }
    }

    private static CompiledMolangExpression instantiate(byte[] classBytes) throws Exception {
        ByteArrayClassLoader loader =
                new ByteArrayClassLoader(MolangCompilerImpl.class.getClassLoader());
        Class<?> clazz = loader.define(classBytes);
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        Object instance = constructor.newInstance();
        return (CompiledMolangExpression) instance;
    }

    private static final class ByteArrayClassLoader extends ClassLoader {
        private ByteArrayClassLoader(ClassLoader parent) {
            super(parent);
        }

        private Class<?> define(byte[] bytes) {
            return defineClass(null, bytes, 0, bytes.length);
        }
    }
}
