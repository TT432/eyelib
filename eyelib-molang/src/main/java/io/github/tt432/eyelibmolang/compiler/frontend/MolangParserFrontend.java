package io.github.tt432.eyelibmolang.compiler.frontend;

import io.github.tt432.eyelibmolang.generated.MolangLexer;
import io.github.tt432.eyelibmolang.generated.MolangParser;

import java.util.function.Consumer;

public interface MolangParserFrontend {
    default MolangParserFrontendResult parseExprSet(String source) {
        return parseExprSet(source, lexer -> {
        }, parser -> {
        });
    }

    MolangParserFrontendResult parseExprSet(
            String source,
            Consumer<MolangLexer> lexerConfigurator,
            Consumer<MolangParser> parserConfigurator
    );
}
