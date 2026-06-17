package io.github.tt432.eyelib.molang.compiler.frontend;

import io.github.tt432.eyelib.molang.compiler.frontend.ast.MolangAst;
import java.util.Optional;

/**
 * @author TT432
 */
public record MolangParserFrontendResult(
        Optional<MolangAst.ExprSet> ast
) {
}
