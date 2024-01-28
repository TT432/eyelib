package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.grammer.MolangLexer;
import io.github.tt432.eyelib.molang.grammer.MolangParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * @author TT432
 */
public record MolangValue(
        MolangSystemScope scope,
        ParseTree context
) {
    public static final float TRUE = 1;
    public static final float FALSE = 0;

    public static final MolangValue TRUE_VALUE = MolangValue.parse(MolangSystemScope.NONE, "1");
    public static final MolangValue FALSE_VALUE = MolangValue.parse(MolangSystemScope.NONE, "0");

    public static MolangValue parse(MolangSystemScope scope, String sourceText) {
        if (sourceText.isBlank()) {
            return FALSE_VALUE;
        }

        MolangParser molangParser = new MolangParser(new CommonTokenStream(new MolangLexer(CharStreams.fromString(sourceText))));
        return new MolangValue(scope, molangParser.exprSet());
    }

    private static final MolangEvalVisitor VISITOR = new MolangEvalVisitor();

    public float eval() {
        VISITOR.setScope(scope.getScope());
        Float accept = context.accept(VISITOR);

        if (accept == null) {
            return 0;
        }

        return accept;
    }

    public boolean evalAsBool() {
        return eval() != FALSE;
    }
}
