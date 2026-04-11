package io.github.tt432.eyelib.core.util.time;

/**
 * Platform-free fixed-step timer state.
 */
public final class FixedStepTimerState {
    private final int rate;
    private boolean init;
    private int lastFixed;
    private int startTicks;
    private float startPartialTick;

    public FixedStepTimerState(int rate) {
        if (rate <= 0) {
            throw new IllegalArgumentException("rate must be > 0");
        }
        this.rate = rate;
    }

    public int getRate() {
        return rate;
    }

    public int getLastFixed() {
        return lastFixed;
    }

    public boolean canNextStep(int ticks, float partialTick) {
        float secondsSinceStart = realSeconds(ticks, partialTick);
        int currentFixed = (int) Math.floor(secondsSinceStart * rate);

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

    public void start(int ticks, float partialTick) {
        this.startTicks = ticks;
        this.startPartialTick = partialTick;
        this.lastFixed = 0;
        this.init = false;
    }

    public float realSeconds(int ticks, float partialTick) {
        return ((ticks + partialTick) - (startTicks + startPartialTick)) / 20f;
    }

    public float seconds() {
        return lastFixed / (float) rate;
    }
}
