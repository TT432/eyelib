package io.github.tt432.eyelib.molang.compiler.frontend;

/**
 * 解析前端选择器，返回当前启用的前端。
 *
 * @author TT432
 */
public final class MolangParserFrontends {
    private static final MolangParserFrontend ACTIVE = new HandwrittenMolangAstParserFrontend();

    private MolangParserFrontends() {
    }

    public static MolangParserFrontend active() {
        return ACTIVE;
    }
}