package io.github.tt432.eyelibmolang.compiler.frontend;

import org.jspecify.annotations.NullMarked;

/**
 * Molang 解析前端接口。
 *
 * @author TT432
 */
@NullMarked
public interface MolangParserFrontend {
    MolangParserFrontendResult parseExprSet(String source);
}
