package io.github.tt432.eyelibmolang.compiler.corpus;

import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangParseResult;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangParseShape;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangDiagnostic;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangDiagnosticPhase;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangDiagnosticSeverity;
import io.github.tt432.eyelibmolang.compiler.frontend.MolangParserFrontendResult;
import io.github.tt432.eyelibmolang.compiler.frontend.MolangParserFrontends;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** @author TT432 */
final class MolangCorpusParseRunner {
    MolangParseResult parseOnly(String source) {
        List<MolangDiagnostic> diagnostics = new ArrayList<>();
        MolangParserFrontendResult parseResult = MolangParserFrontends.active().parseExprSet(source);

        // 当手写前端无法生成 AST 且 source 非空时，通过合成诊断指示失败
        if (parseResult.ast().isEmpty() && !source.trim().isEmpty()) {
            diagnostics.add(new MolangDiagnostic(
                    MolangDiagnosticPhase.PARSER,
                    MolangDiagnosticSeverity.ERROR,
                    "PARSER_SYNTAX_ERROR",
                    "Frontend completed without producing an AST."
            ));
        }

        return new MolangParseResult(
                parseResult.ast().isEmpty() && !source.trim().isEmpty(),
                diagnostics,
                new MolangParseShape("", Set.of()),
                parseResult.ast()
        );
    }
}
