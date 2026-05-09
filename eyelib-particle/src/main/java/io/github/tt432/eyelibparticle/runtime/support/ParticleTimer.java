package io.github.tt432.eyelibparticle.runtime.support;

import io.github.tt432.eyelibparticle.runtime.ParticleRuntimeServices;

import java.util.Objects;

/**
 * Platform-free fixed-step timer for particle runtime extraction.
 */
public final class ParticleTimer {
    private static final int DEFAULT_RATE = 30;

    private final ParticleRuntimeServices.TimeSource timeSource;
    private final int rate;
    private boolean init;
    private int lastFixed;
    private int startTicks;
    private float startPartialTick;

    public ParticleTimer(ParticleRuntimeServices.TimeSource timeSource) {
        this(timeSource, DEFAULT_RATE);
    }

    public ParticleTimer(ParticleRuntimeServices.TimeSource timeSource, int rate) {
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        if (rate <= 0) {
            throw new IllegalArgumentException("rate must be > 0");
        }
        this.rate = rate;
    }

    public int rate() {
        return rate;
    }

    public int lastFixed() {
        return lastFixed;
    }

    public void start() {
        startTicks = timeSource.ticks();
        startPartialTick = timeSource.partialTick();
        lastFixed = 0;
        init = false;
    }

    public boolean canNextStep() {
        int currentFixed = (int) Math.floor(realSec() * rate);

        if (currentFixed > lastFixed) {
            lastFixed += 1;
            return true;
        }

        if (!init) {
            init = true;
            lastFixed = 1;
            return true;
        }

        return false;
    }

    public float realSec() {
        return ((timeSource.ticks() + timeSource.partialTick()) - (startTicks + startPartialTick)) / 20F;
    }

    public float seconds() {
        return lastFixed / (float) rate;
    }
}
