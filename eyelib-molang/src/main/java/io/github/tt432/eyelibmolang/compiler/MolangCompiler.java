package io.github.tt432.eyelibmolang.compiler;

import org.jspecify.annotations.NullMarked;

/**
 * Molang 表达式编译器。
 *
 * @author TT432
 */
@NullMarked
/** @author TT432 */
public interface MolangCompiler {
    CompiledMolangExpression compile(String expression, CompileContext ctx);
}