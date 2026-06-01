package io.github.tt432.eyelibmolang.compiler.frontend;

import io.github.tt432.eyelibmolang.generated.MolangLexer;
import io.github.tt432.eyelibmolang.generated.MolangParser;
import org.jspecify.annotations.NullMarked;

import java.util.function.Consumer;

/**
 * Molang 解析前端接口。
 *
 * @author TT432
 */
@NullMarked
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