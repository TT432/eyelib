package io.github.tt432.eyelib.client.animation.bedrock;

import io.github.tt432.eyelibimporter.animation.bedrock.BrLoopType;


public final class BrAnimationPlaybackState {
    private int loopedTimes;
    private float lastTicks;
    private float animTime;
    private float deltaTime;

    public TickResult tick(BrLoopType loopType, float animationLength, float ticks, float animTimeUpdate) {
        if (lastTicks == 0) {
            lastTicks = ticks;
        }

        deltaTime = ticks - lastTicks;
        lastTicks = ticks;
        animTime = animTimeUpdate;

        if (animationLength <= 0) {
            return new TickResult(animTimeUpdate, false);
        }

        return switch (loopType) {
            case LOOP -> {
                int nextLoopedTimes = (int) (animTimeUpdate / animationLength);
                boolean loopRestarted = nextLoopedTimes > loopedTimes;
                if (loopRestarted) {
                    loopedTimes = nextLoopedTimes;
                }
                yield new TickResult(animTimeUpdate % animationLength, loopRestarted);
            }
            case ONCE -> new TickResult(animTimeUpdate, false);
            default -> new TickResult(Math.min(animTimeUpdate, animationLength), false);
        };
    }

    public void reset() {
        loopedTimes = 0;
        lastTicks = 0;
        animTime = 0;
        deltaTime = 0;
    }

    public boolean anyAnimationFinished(float animationLength) {
        return loopedTimes > 0 || animTime > animationLength;
    }

    public int loopedTimes() {
        return loopedTimes;
    }

    public float lastTicks() {
        return lastTicks;
    }

    public float animTime() {
        return animTime;
    }

    public float deltaTime() {
        return deltaTime;
    }

    public record TickResult(float animTick, boolean loopRestarted) {
    }
}
