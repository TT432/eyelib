package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.compiler.binding.BoundMolang;

public record BoundMolangCompilerInput(
        String sourceExpression,
        BoundMolang.BoundExprSet root,
        CompileContext context
) {
}
