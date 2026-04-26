package io.github.tt432.eyelibmolang.compiler.frontend;

import io.github.tt432.eyelibmolang.generated.MolangLexer;
import io.github.tt432.eyelibmolang.generated.MolangParser;

import java.util.Optional;
import java.util.function.Consumer;

public final class GeneratedParserBackedAstMolangParserFrontend implements MolangParserFrontend {
    public static final GeneratedParserBackedAstMolangParserFrontend INSTANCE = new GeneratedParserBackedAstMolangParserFrontend();

    private GeneratedParserBackedAstMolangParserFrontend() {
    }

    @Override
    public MolangParserFrontendResult parseExprSet(
            String source,
            Consumer<MolangLexer> lexerConfigurator,
            Consumer<MolangParser> parserConfigurator
    ) {
        MolangParserFrontendResult parseResult = GeneratedMolangParserFrontend.INSTANCE.parseExprSet(source, lexerConfigurator, parserConfigurator);
        if (parseResult.ast().isPresent()) {
            return parseResult;
        }
        return new MolangParserFrontendResult(
                parseResult.parser(),
                parseResult.exprSet(),
                Optional.of(new GeneratedMolangAstBuilder().build(parseResult.exprSet()))
        );
    }
}
