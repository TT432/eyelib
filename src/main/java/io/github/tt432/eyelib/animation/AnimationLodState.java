package io.github.tt432.eyelib.animation;

import org.jspecify.annotations.Nullable;

/**
 * Schedules pose sampling independently from animation state/effect ticking and smooths sampled poses.
 */
public final class AnimationLodState {
    private final ModelRuntimeData interpolationStart = new ModelRuntimeData();
    private final ModelRuntimeData interpolationTarget = new ModelRuntimeData();
    private final ModelRuntimeData interpolated = new ModelRuntimeData();

    private ModelRuntimeData lastOutput = ModelRuntimeData.EMPTY;
    private long lastSampleFrame = Long.MIN_VALUE;
    private int interval = 1;
    private float interpolationProgress = 1F;
    private boolean initialized;

    public boolean shouldSample(long frameIndex, int requestedInterval) {
        int normalizedInterval = Math.max(1, requestedInterval);
        return normalizedInterval == 1
                || !initialized
                || normalizedInterval != interval
                || frameIndex - lastSampleFrame >= normalizedInterval;
    }

    public ModelRuntimeData resolve(long frameIndex, int requestedInterval, @Nullable ModelRuntimeData sampledPose) {
        int normalizedInterval = Math.max(1, requestedInterval);
        if (normalizedInterval == 1) {
            if (sampledPose == null) {
                return lastOutput;
            }
            interval = 1;
            lastSampleFrame = frameIndex;
            interpolationProgress = 1F;
            initialized = true;
            lastOutput = sampledPose;
            return sampledPose;
        }

        if (sampledPose != null) {
            lastSampleFrame = frameIndex;
            interval = normalizedInterval;
            if (!initialized) {
                interpolated.set(sampledPose);
                interpolationStart.set(sampledPose);
                interpolationTarget.set(sampledPose);
                interpolationProgress = 1F;
                initialized = true;
                lastOutput = interpolated;
                return interpolated;
            }

            interpolationStart.set(lastOutput);
            interpolationTarget.set(sampledPose);
            interpolationProgress = 0F;
        }

        if (!initialized) {
            return ModelRuntimeData.EMPTY;
        }

        interpolationProgress = Math.min(1F, interpolationProgress + 1F / interval);
        interpolated.interpolate(interpolationStart, interpolationTarget, interpolationProgress);
        lastOutput = interpolated;
        return interpolated;
    }

    public void reset() {
        interpolationStart.resetAndClear();
        interpolationTarget.resetAndClear();
        interpolated.resetAndClear();
        lastOutput = ModelRuntimeData.EMPTY;
        lastSampleFrame = Long.MIN_VALUE;
        interval = 1;
        interpolationProgress = 1F;
        initialized = false;
    }
}
