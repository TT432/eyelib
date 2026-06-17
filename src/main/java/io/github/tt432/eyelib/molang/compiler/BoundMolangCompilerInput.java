package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.compiler.binding.BoundMolang;
/**
 * @author TT432
 */
public record BoundMolangCompilerInput(
        String sourceExpression,
        BoundMolang.BoundExprSet root,
        CompileContext context
) {
}