package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.grammer.MolangLexer;
import io.github.tt432.eyelib.molang.grammer.MolangParser;
import lombok.Getter;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * @author TT432
 */
@Getter
public class MolangValue {
    public static final float TRUE = 1;
    public static final float FALSE = 0;

    public static final MolangValue TRUE_VALUE = MolangValue.parse(null, "1");
    public static final MolangValue FALSE_VALUE = MolangValue.parse(null, "0");

    MolangScope scope;
    final ParseTree context;

    private MolangValue(ParseTree context) {
        this.context = context;
    }

    public static MolangValue parse(MolangScope scope, String sourceText) {
        if (sourceText.isBlank()) {
            return FALSE_VALUE;
        }

        MolangParser molangParser = new MolangParser(new CommonTokenStream(new MolangLexer(CharStreams.fromString(sourceText))));
        MolangValue result = new MolangValue(molangParser.exprSet());
        result.scope = scope;
        return result;
    }

    private static final MolangEvalVisitor VISITOR = new MolangEvalVisitor();

    public float eval() {
        VISITOR.setScope(scope);
        Float accept = context.accept(VISITOR);

        if (accept == null) {
            return 0;
        }

        return accept;
    }

    public boolean evalAsBool() {
        return eval() != FALSE;
    }

    public MolangValue copy(MolangScope scope) {
        MolangValue molangValue = new MolangValue(context);
        molangValue.scope = scope;
        return molangValue;
    }
}
