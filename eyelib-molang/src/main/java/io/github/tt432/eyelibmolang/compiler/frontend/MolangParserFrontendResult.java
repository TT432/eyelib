package io.github.tt432.eyelibmolang.compiler.frontend;

import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelibmolang.generated.MolangParser;

import java.util.Optional;

public record MolangParserFrontendResult(
        MolangParser parser,
        MolangParser.ExprSetContext exprSet,
        Optional<MolangAst.ExprSet> ast
) {
}
