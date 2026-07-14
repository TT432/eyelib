package io.github.tt432.eyelib.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnimationLodStateTest {
    @Test
    void reducedRateSamplesOnConfiguredFrameInterval() {
        AnimationLodState state = new AnimationLodState();

        assertTrue(state.shouldSample(0, 4));
        state.resolve(0, 4, pose(0F));
        assertFalse(state.shouldSample(1, 4));
        assertFalse(state.shouldSample(3, 4));
        assertTrue(state.shouldSample(4, 4));
    }

    @Test
    void sampledPoseIsInterpolatedAcrossSkippedFrames() {
        AnimationLodState state = new AnimationLodState();
        ModelRuntimeData first = state.resolve(0, 2, pose(0F));
        assertEquals(0F, first.getData(7).position.x, 0.0001F);

        ModelRuntimeData halfway = state.resolve(2, 2, pose(2F));
        assertEquals(1F, halfway.getData(7).position.x, 0.0001F);

        ModelRuntimeData target = state.resolve(3, 2, null);
        assertEquals(2F, target.getData(7).position.x, 0.0001F);
    }

    @Test
    void fullRateReturnsCurrentSampleWithoutLag() {
        AnimationLodState state = new AnimationLodState();
        ModelRuntimeData sample = pose(3F);

        assertSame(sample, state.resolve(0, 1, sample));
    }

    @Test
    void effectsOnlyDataNeverCreatesPoseEntries() {
        ModelRuntimeData effectsOnly = ModelRuntimeData.effectsOnly();

        assertNull(effectsOnly.getDataForAnimation(1));
        assertEquals(0, effectsOnly.entryCount());
    }

    private static ModelRuntimeData pose(float x) {
        ModelRuntimeData result = new ModelRuntimeData();
        result.getData(7).position.x = x;
        return result;
    }
}
