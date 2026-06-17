package io.github.tt432.eyelibmolang.mapping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** @author TT432 */
class MolangEaseFunctionsTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void easeFunctionsReturnRangeEndsAtBoundaries() {
        assertEquals(0F, MolangMath.ease_in_quad(0F, 1F, 0F), EPSILON);
        assertEquals(1F, MolangMath.ease_in_quad(0F, 1F, 1F), EPSILON);
        assertEquals(10F, MolangMath.ease_in_quad(10F, 20F, 0F), EPSILON);
        assertEquals(20F, MolangMath.ease_in_quad(10F, 20F, 1F), EPSILON);
    }

    @Test
    void easeOutBounceReturnsCurveEnds() {
        assertEquals(0F, MolangMath.ease_out_bounce(0F, 1F, 0F), EPSILON);
        assertEquals(1F, MolangMath.ease_out_bounce(0F, 1F, 1F), EPSILON);
    }

    @Test
    void easeInElasticReturnsCurveEnds() {
        assertEquals(0F, MolangMath.ease_in_elastic(0F, 1F, 0F), EPSILON);
        assertEquals(1F, MolangMath.ease_in_elastic(0F, 1F, 1F), EPSILON);
    }

    @Test
    void easeInoutSineReturnsHalfAtMidpoint() {
        assertEquals(0.5F, MolangMath.ease_inout_sine(0F, 1F, 0.5F), EPSILON);
    }
}
