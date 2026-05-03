package io.github.tt432.eyelibmolang.compiler.frontend;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MolangParserFrontendDivergenceTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "1+2",
            "a.b",
            "q.foo(1)",
            "a??b",
            "v.x=1",
            "1>2",
            "1<2",
            "1<=2",
            "1>=2",
            "1==2",
            "1!=2",
            "!a",
            "-b",
            "a&&b",
            "a||b",
            "a?b:c",
            "a?b",
            "return 1",
            "loop(3,{a=1;})",
            "for_each(t.x,arr,{})"
    })
    void activeAndHandwrittenFrontendsAgreeOnAcceptReject(String expression) {
        boolean activeAccepted = MolangParserFrontends.active()
                                                      .parseExprSet(expression)
                                                      .ast()
                                                      .isPresent();
        boolean handwrittenAccepted = HandwrittenMolangAstParserFrontend.INSTANCE
                .parseExprSet(expression)
                .ast()
                .isPresent();

        assertEquals(activeAccepted, handwrittenAccepted, expression);
    }
}
