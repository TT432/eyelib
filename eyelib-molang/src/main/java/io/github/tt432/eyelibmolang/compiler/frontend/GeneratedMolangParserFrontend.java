package io.github.tt432.eyelibmolang.compiler.frontend;

import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import io.github.tt432.eyelibmolang.generated.MolangLexer;
import io.github.tt432.eyelibmolang.generated.MolangParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.Optional;
import java.util.function.Consumer;

public final class GeneratedMolangParserFrontend implements MolangParserFrontend {
    public static final GeneratedMolangParserFrontend INSTANCE = new GeneratedMolangParserFrontend();

    private GeneratedMolangParserFrontend() {
    }

    @Override
    public MolangParserFrontendResult parseExprSet(
            String source,
            Consumer<MolangLexer> lexerConfigurator,
            Consumer<MolangParser> parserConfigurator
    ) {
        MolangLexer lexer = new MolangLexer(CharStreams.fromString(source));
        lexerConfigurator.accept(lexer);

        MolangParser parser = new MolangParser(new CommonTokenStream(lexer));
        parserConfigurator.accept(parser);

        MolangParser.ExprSetContext exprSet = parser.exprSet();
        Optional<MolangAst.ExprSet> ast = Optional.of(new GeneratedMolangAstBuilder().build(exprSet));

        return new MolangParserFrontendResult(parser, exprSet, ast);
    }
}
