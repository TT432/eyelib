package io.github.tt432.eyelibmolang.compiler.corpus;

import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangParseResult;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangParseShape;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangDiagnostic;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangDiagnosticPhase;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangDiagnosticSeverity;
import io.github.tt432.eyelibmolang.compiler.frontend.MolangParserFrontendResult;
import io.github.tt432.eyelibmolang.compiler.frontend.MolangParserFrontends;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** @author TT432 */
final class MolangCorpusParseRunner {
    MolangParseResult parseOnly(String source) {
        List<MolangDiagnostic> diagnostics = new ArrayList<>();

        MolangParserFrontendResult parseResult = MolangParserFrontends.active().parseExprSet(
                source,
                lexer -> {
                    lexer.removeErrorListeners();
                    lexer.addErrorListener(new CollectingErrorListener(MolangDiagnosticPhase.LEXER, "LEXER_SYNTAX_ERROR", diagnostics));
                },
                parser -> {
                    parser.removeErrorListeners();
                    parser.addErrorListener(new CollectingErrorListener(MolangDiagnosticPhase.PARSER, "PARSER_SYNTAX_ERROR", diagnostics));
                }
        );

        ParserRuleContext root = parseResult.exprSet();
        if (parseResult.parser().getCurrentToken().getType() != Token.EOF) {
            diagnostics.add(new MolangDiagnostic(
                    MolangDiagnosticPhase.PARSER,
                    MolangDiagnosticSeverity.ERROR,
                    "PARSER_SYNTAX_ERROR",
                    "Trailing token remains after parse completion."
            ));
        }

        return new MolangParseResult(!diagnostics.isEmpty(), diagnostics, collectParseShape(root), parseResult.ast());
    }

    private MolangParseShape collectParseShape(ParserRuleContext root) {
        Set<String> ruleNames = new LinkedHashSet<>();
        collectRuleNames(root, ruleNames);
        return new MolangParseShape(ruleName(root), ruleNames);
    }

    private void collectRuleNames(ParseTree node, Set<String> ruleNames) {
        if (node instanceof ParserRuleContext context) {
            ruleNames.add(ruleName(context));
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectRuleNames(node.getChild(i), ruleNames);
        }
    }

    private String ruleName(ParserRuleContext context) {
        String simpleName = context.getClass().getSimpleName();
        if (simpleName.endsWith("Context")) {
            return simpleName.substring(0, simpleName.length() - "Context".length());
        }
        return simpleName;
    }

    private static final class CollectingErrorListener extends BaseErrorListener {
        private final MolangDiagnosticPhase phase;
        private final String code;
        private final List<MolangDiagnostic> diagnostics;

        private CollectingErrorListener(MolangDiagnosticPhase phase, String code, List<MolangDiagnostic> diagnostics) {
            this.phase = phase;
            this.code = code;
            this.diagnostics = diagnostics;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            diagnostics.add(new MolangDiagnostic(
                    phase,
                    MolangDiagnosticSeverity.ERROR,
                    code,
                    "line=" + line + ", col=" + charPositionInLine + ", msg=" + msg
            ));
        }
    }
}