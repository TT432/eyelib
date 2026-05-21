package io.github.tt432.eyelibmolang.compiler.frontend;

import io.github.tt432.eyelibmolang.generated.MolangLexer;
import io.github.tt432.eyelibmolang.generated.MolangParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jspecify.annotations.NullMarked;

import java.util.function.Consumer;

/**
 * 基于 ANTLR 生成解析器的解析前端。
 *
 * @author TT432
 */
@NullMarked
/** @author TT432 */
public final class GeneratedParserBackedMolangParserFrontend implements MolangParserFrontend {
    public static final GeneratedParserBackedMolangParserFrontend INSTANCE = new GeneratedParserBackedMolangParserFrontend();

    private GeneratedParserBackedMolangParserFrontend() {
    }

    @Override
    public MolangParserFrontendResult parseExprSet(String source,
                                                   Consumer<MolangLexer> lexerConfigurator,
                                                   Consumer<MolangParser> parserConfigurator) {
        MolangLexer lexer = new MolangLexer(CharStreams.fromString(source));
        lexerConfigurator.accept(lexer);
        MolangParser parser = new MolangParser(new CommonTokenStream(lexer));
        parserConfigurator.accept(parser);
        MolangParser.ExprSetContext exprSet = parser.exprSet();
        return new MolangParserFrontendResult(
                parser,
                exprSet,
                HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(source)
        );
    }
}