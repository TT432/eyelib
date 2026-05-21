package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.compiler.binding.BoundMolang;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
/** @author TT432 */
public record BoundMolangCompilerInput(
        String sourceExpression,
        BoundMolang.BoundExprSet root,
        CompileContext context
) {
}