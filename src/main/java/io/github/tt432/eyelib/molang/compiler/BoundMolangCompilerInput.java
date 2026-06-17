package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.compiler.binding.BoundMolang;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public record BoundMolangCompilerInput(
        String sourceExpression,
        BoundMolang.BoundExprSet root,
        CompileContext context
) {
}