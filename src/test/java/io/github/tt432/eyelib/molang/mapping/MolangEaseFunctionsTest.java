package io.github.tt432.eyelib.molang.mapping;

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

    @Test
    void easeInoutBackOvershootsBothEndsAndIsSmoothAtMidpoint() {
        // 端点精确落位
        assertEquals(0F, MolangMath.ease_inout_back(0F, 1F, 0F), EPSILON);
        assertEquals(1F, MolangMath.ease_inout_back(0F, 1F, 1F), EPSILON);
        // 中段无溢出：t=0.5 恰在中点
        assertEquals(0.5F, MolangMath.ease_inout_back(0F, 1F, 0.5F), EPSILON);
        assertEquals(15F, MolangMath.ease_inout_back(10F, 20F, 0.5F), EPSILON);
        // 两端 overshoot：曲线越过 start（负向）与越过 end（正向）
        assertEquals(-0.0375F, MolangMath.ease_inout_back(0F, 1F, 0.1F), 0.001F);
        assertEquals(1.0375F, MolangMath.ease_inout_back(0F, 1F, 0.9F), 0.001F);
    }
}
