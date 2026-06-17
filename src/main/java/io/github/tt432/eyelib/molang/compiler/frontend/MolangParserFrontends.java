package io.github.tt432.eyelibmolang.compiler.frontend;

import org.jspecify.annotations.NullMarked;

/**
 * 解析前端选择器，返回当前启用的前端。
 *
 * @author TT432
 */
@NullMarked
public final class MolangParserFrontends {
    private static final MolangParserFrontend ACTIVE = HandwrittenMolangAstParserFrontend.INSTANCE;

    private MolangParserFrontends() {
    }

    public static MolangParserFrontend active() {
        return ACTIVE;
    }
}