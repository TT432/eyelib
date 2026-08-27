package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.compiler.binding.BindDiagnostic;
import io.github.tt432.eyelib.molang.compiler.binding.BindResult;
import io.github.tt432.eyelib.molang.compiler.binding.MolangBinder;
import io.github.tt432.eyelib.molang.compiler.frontend.MolangParserFrontends;
import io.github.tt432.eyelib.molang.compiler.frontend.MolangParserFrontendResult;
import io.github.tt432.eyelib.molang.compiler.frontend.ast.MolangAst;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup.ClassOption;
import java.lang.invoke.MethodType;
import java.util.List;

/**
 * Molang 编译器实现：解析 → 绑定 → 生成字节码 → 加载。
 *
 * @author TT432
 */
public final class MolangCompilerImpl implements MolangCompiler {
    private static final MolangBinder BINDER = new MolangBinder();

    @Override
    public CompiledMolangExpression compile(String expression, CompileContext ctx) {
        CompileContext effectiveCtx = ctx == null ? CompileContext.defaults() : ctx;
        try {
            // 步骤 1：通过统一前端入口将源码解析为 AST
            MolangParserFrontendResult parseResult = MolangParserFrontends.active()
                    .parseExprSet(expression);
            MolangAst.ExprSet ast = parseResult.ast()
                    .orElseThrow(() ->
                            new ExpressionCompileException(expression,
                                    "Failed to parse molang expression: [" + expression + "]"));

            // 步骤 2：绑定 AST — 解析标识符、验证语义
            BindResult bindResult = BINDER.bind(ast, effectiveCtx.diagnosticsMode());
            if (bindResult.hasErrors()) {
                List<String> diagnostics = bindResult.diagnostics().stream()
                        .filter(d -> d.severity() == BindDiagnostic.Severity.ERROR)
                        .map(BindDiagnostic::message)
                        .toList();
                throw new ExpressionCompileException(expression,
                        "Failed to bind molang expression: semantic errors detected.", diagnostics);
            }

            // 步骤 3：从绑定后的 AST 生成 JVM 字节码
            BoundMolangCompilerInput input = new BoundMolangCompilerInput(
                    expression, bindResult.root(), effectiveCtx);
            byte[] classBytes = MolangBytecodeEmitter.emit(input);

            // 步骤 4：加载生成的类并包装为 CompiledMolangExpression
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

        // 生成类已实现 CompiledMolangExpression 全部方法，直接返回实例：
        // 旧实现经 bindTo+dropArguments 包一层 MethodHandle，evaluate 走 LambdaForm invoke
        // （JFR 实证：Invokers.checkCustomized ~9% + HiddenMolangExpression.evaluate ~10% 渲染线程），
        // 直接接口调用由 JIT vtable 分派，零 LambdaForm 开销；异常由 MolangValue.getObject 统一捕获记录。
        return (CompiledMolangExpression) hiddenInstance;
    }
}