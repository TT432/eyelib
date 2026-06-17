package io.github.tt432.eyelib.molang.compiler;

/**
 * Molang 表达式编译器。
 *
 * @author TT432
 */
public interface MolangCompiler {
    CompiledMolangExpression compile(String expression, CompileContext ctx);
}