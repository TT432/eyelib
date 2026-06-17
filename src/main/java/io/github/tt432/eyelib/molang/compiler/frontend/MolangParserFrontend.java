package io.github.tt432.eyelib.molang.compiler.frontend;

/**
 * Molang 解析前端接口。
 *
 * @author TT432
 */
public interface MolangParserFrontend {
    MolangParserFrontendResult parseExprSet(String source);
}
