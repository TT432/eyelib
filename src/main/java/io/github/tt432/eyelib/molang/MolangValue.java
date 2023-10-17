package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.grammer.MolangLexer;
import io.github.tt432.eyelib.molang.grammer.MolangParser;
import lombok.Getter;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;

/**
 * @author TT432
 */
@Getter
public class MolangValue {
    public static final float TRUE = 1;
    public static final float FALSE = 0;

    MolangScope scope;
    final MolangParser.ExprSetContext context;

    private MolangValue(MolangParser.ExprSetContext context) {
        this.context = context;
    }

    public static MolangValue parse(MolangScope scope, String sourceText) {
        MolangValue result = new MolangValue(new MolangParser(new BufferedTokenStream(new MolangLexer(CharStreams.fromString(sourceText)))).exprSet());
        result.scope = scope;
        return result;
    }

    public float eval() {
        return context.accept(new MolangEvalVisitor(scope));
    }

    public MolangValue copy(MolangScope scope) {
        MolangValue molangValue = new MolangValue(context);
        molangValue.scope = scope;
        return molangValue;
    }
}
