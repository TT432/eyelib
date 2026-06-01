package io.github.tt432.eyelibmolang.compiler.frontend;

import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelibmolang.generated.MolangParser;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;

/**
 * @author TT432
 */
@NullMarked
public record MolangParserFrontendResult(
        MolangParser parser,
        MolangParser.ExprSetContext exprSet,
        Optional<MolangAst.ExprSet> ast
) {
}