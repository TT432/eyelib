package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.compiler.binding.BindDiagnostic;
import io.github.tt432.eyelibmolang.compiler.binding.BindResult;
import io.github.tt432.eyelibmolang.compiler.binding.MolangBinder;
import io.github.tt432.eyelibmolang.compiler.frontend.MolangParserFrontends;
import io.github.tt432.eyelibmolang.compiler.frontend.MolangParserFrontendResult;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.type.MolangObject;
import org.jspecify.annotations.NullMarked;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup.ClassOption;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Set;

/**
 * Molang 编译器实现：解析 → 绑定 → 生成字节码 → 加载。
 *
 * @author TT432
 */
@NullMarked
/** @author TT432 */
public final class MolangCompilerImpl implements MolangCompiler {
    private static final MolangBinder BINDER = new MolangBinder();

    @Override
    public CompiledMolangExpression compile(String expression, CompileContext ctx) {
        CompileContext effectiveCtx = ctx == null ? CompileContext.defaults() : ctx;
        try {
            // Step 1: Parse source into AST via unified frontend entry
            MolangParserFrontendResult parseResult = MolangParserFrontends.active()
                    .parseExprSet(expression);
            MolangAst.ExprSet ast = parseResult.ast()
                    .orElseThrow(() ->
                            new ExpressionCompileException(expression,
                                    "Failed to parse molang expression: [" + expression + "]"));

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

    private static CompiledMolangExpression instantiate(byte[] classBytes) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup()
                .defineHiddenClass(classBytes, true, ClassOption.NESTMATE);
        Class<?> hiddenClass = lookup.lookupClass();

        Object hiddenInstance = lookup
                .findConstructor(hiddenClass, MethodType.methodType(void.class))
                .invoke();

        CompiledMolangExpression metadata = (CompiledMolangExpression) hiddenInstance;
        MethodHandle evaluateHandle = lookup
                .findVirtual(hiddenClass, "evaluate",
                        MethodType.methodType(MolangObject.class, MolangScope.class))
                .bindTo(hiddenInstance);
        MethodHandle wrappedHandle = MethodHandles.dropArguments(
                evaluateHandle, 0, HiddenMolangExpression.class);

        return new HiddenMolangExpression(
                metadata.sourceExpression(),
                wrappedHandle,
                metadata.requiredHostRoles());
    }

    private record HiddenMolangExpression(
            String sourceExpression,
            MethodHandle evaluateHandle,
            Set<String> requiredHostRoles
    ) implements CompiledMolangExpression {
        @Override
        public MolangObject evaluate(MolangScope scope) {
            try {
                return (MolangObject) evaluateHandle.invoke(this, scope);
            } catch (Throwable t) {
                throw new IllegalStateException(
                        "Failed to invoke hidden Molang expression: " + sourceExpression, t);
            }
        }
    }
}