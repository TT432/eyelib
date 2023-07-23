package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.grammer.MolangLexer;
import io.github.tt432.eyelib.molang.grammer.MolangParser;
import lombok.Getter;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;

/**
 * @author TT432
 */
public class MolangValue {
    public static final float TRUE = 1;
    public static final float FALSE = 0;

    @Getter
    MolangScope scope;
    @Getter
    MolangParser.ExprSetContext context;

    public static MolangValue parse(MolangScope scope, String sourceText) {
        MolangValue result = new MolangValue();
        result.context = new MolangParser(new BufferedTokenStream(new MolangLexer(CharStreams.fromString(sourceText)))).exprSet();
        result.scope = scope;
        return result;
    }

    public float eval() {
        return context.accept(new MolangEvalVisitor(scope));
    }
}
