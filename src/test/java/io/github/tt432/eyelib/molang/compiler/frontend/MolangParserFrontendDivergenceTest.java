package io.github.tt432.eyelib.molang.compiler.frontend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author TT432
 */
class MolangParserFrontendDivergenceTest {

    @Test
    void activeFrontendIsHandwrittenInstance() {
        assertSame(
                HandwrittenMolangAstParserFrontend.INSTANCE,
                MolangParserFrontends.active(),
                "active() 应当返回 HandwrittenMolangAstParserFrontend.INSTANCE"
        );
    }
}
