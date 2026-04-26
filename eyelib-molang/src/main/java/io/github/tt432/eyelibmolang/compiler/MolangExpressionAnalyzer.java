package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.compiler.frontend.MolangParserFrontendResult;
import io.github.tt432.eyelibmolang.compiler.frontend.MolangParserFrontends;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MolangExpressionAnalyzer {
    private static final Map<String, MolangExpressionAnalysis> DEFAULT_CACHE = new ConcurrentHashMap<>();

    private MolangExpressionAnalyzer() {
    }

    public static MolangExpressionAnalysis analyze(String expression) {
        String normalized = expression == null ? "" : expression.trim();
        return DEFAULT_CACHE.computeIfAbsent(normalized, key -> analyze(key, MolangExpressionEnvironment.DEFAULT));
    }

    public static void clearDefaultCache() {
        DEFAULT_CACHE.clear();
    }

    public static MolangExpressionAnalysis analyze(String expression, MolangExpressionEnvironment environment) {
        String normalized = expression == null ? "" : expression.trim();
        if (normalized.isBlank()) {
            return MolangExpressionAnalysis.constant();
        }

        ensureMappingsInitialized();

        ParsingErrorListener errorListener = new ParsingErrorListener();
        MolangParserFrontendResult parseResult = MolangParserFrontends.active().parseExprSet(
                normalized,
                lexer -> {
                },
                parser -> {
                    parser.removeErrorListeners();
                    parser.addErrorListener(errorListener);
                }
        );

        MolangExpressionAnalysis analysis = new MolangExpressionAnalysisVisitor(environment).visit(parseResult.exprSet());
        if (errorListener.message != null) {
            return MolangExpressionAnalysis.dynamic("syntax_error:" + errorListener.message);
        }

        return analysis;
    }

    static void ensureMappingsInitialized() {
        if (isTreeInitialized()) {
            return;
        }

        synchronized (MolangMappingTree.INSTANCE) {
            if (isTreeInitialized()) {
                return;
            }

            MolangMappingTree.setupMolangMappingTree(List::of);
        }
    }

    private static boolean isTreeInitialized() {
        MolangMappingTree.Node root = MolangMappingTree.INSTANCE.toplevelNode;
        return !root.children.isEmpty() || !root.actualClasses.isEmpty() || !root.actualFunctions.isEmpty();
    }

    private static final class ParsingErrorListener extends BaseErrorListener {
        private String message;

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                                String msg, RecognitionException e) {
            message = msg;
        }
    }
}
