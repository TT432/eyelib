package io.github.tt432.eyelib.client.animation.bedrock;

import io.github.tt432.eyelibimporter.animation.bedrock.BrLoopType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrAnimationPlaybackStateTest {
    @Test
    void loopTickWrapsAndMarksRestartWhenLoopBoundaryCrosses() {
        BrAnimationPlaybackState state = new BrAnimationPlaybackState();

        BrAnimationPlaybackState.TickResult first = state.tick(BrLoopType.LOOP, 5F, 1F, 1F);
        BrAnimationPlaybackState.TickResult second = state.tick(BrLoopType.LOOP, 5F, 6F, 6F);

        assertEquals(1F, first.animTick(), 0.0001F);
        assertFalse(first.loopRestarted());
        assertEquals(1F, second.animTick(), 0.0001F);
        assertTrue(second.loopRestarted());
        assertEquals(5F, state.deltaTime(), 0.0001F);
        assertEquals(6F, state.animTime(), 0.0001F);
        assertTrue(state.anyAnimationFinished(5F));
    }

    @Test
    void holdOnLastFrameClampsAtAnimationLength() {
        BrAnimationPlaybackState state = new BrAnimationPlaybackState();

        state.tick(BrLoopType.HOLD_ON_LAST_FRAME, 3F, 1F, 1F);
        BrAnimationPlaybackState.TickResult result = state.tick(BrLoopType.HOLD_ON_LAST_FRAME, 3F, 5F, 8F);

        assertEquals(3F, result.animTick(), 0.0001F);
        assertFalse(result.loopRestarted());
    }

    @Test
    void resetClearsPlaybackCounters() {
        BrAnimationPlaybackState state = new BrAnimationPlaybackState();
        state.tick(BrLoopType.LOOP, 2F, 3F, 3F);

        state.reset();

        assertEquals(0, state.loopedTimes());
        assertEquals(0F, state.lastTicks(), 0.0001F);
        assertEquals(0F, state.deltaTime(), 0.0001F);
        assertEquals(0F, state.animTime(), 0.0001F);
        assertFalse(state.anyAnimationFinished(2F));
    }
}
