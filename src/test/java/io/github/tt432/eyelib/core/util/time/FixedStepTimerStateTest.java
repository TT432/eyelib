package io.github.tt432.eyelib.core.util.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedStepTimerStateTest {
    @Test
    void firstStepIsImmediatelyAvailableAfterStart() {
        FixedStepTimerState timer = new FixedStepTimerState(30);
        timer.start(100, 0.25f);

        assertTrue(timer.canNextStep(100, 0.25f));
        assertFalse(timer.canNextStep(100, 0.25f));
    }

    @Test
    void steppedSecondsFollowConfiguredRate() {
        FixedStepTimerState timer = new FixedStepTimerState(30);
        timer.start(0, 0f);

        timer.canNextStep(0, 0f);
        assertEquals(1f / 30f, timer.seconds(), 1.0e-6f);
    }

    @Test
    void catchUpIsOneFixedStepPerCall() {
        FixedStepTimerState timer = new FixedStepTimerState(30);
        timer.start(0, 0f);

        assertTrue(timer.canNextStep(0, 0f));

        int accepted = 0;
        for (int i = 0; i < 29; i++) {
            if (timer.canNextStep(20, 0f)) {
                accepted++;
            }
        }

        assertEquals(29, accepted);
        assertEquals(30, timer.getLastFixed());
    }

    @Test
    void realSecondsUsesTickAndPartialTick() {
        FixedStepTimerState timer = new FixedStepTimerState(30);
        timer.start(10, 0.5f);

        assertEquals(1.0f, timer.realSeconds(30, 0.5f), 1.0e-6f);
        assertEquals(0.5f, timer.realSeconds(20, 0.5f), 1.0e-6f);
    }
}
