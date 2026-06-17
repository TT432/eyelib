package io.github.tt432.eyelib.molang.compiler;

import org.jspecify.annotations.NullMarked;

/**
 * Molang 表达式编译器。
 *
 * @author TT432
 */
@NullMarked
public interface MolangCompiler {
    CompiledMolangExpression compile(String expression, CompileContext ctx);
}