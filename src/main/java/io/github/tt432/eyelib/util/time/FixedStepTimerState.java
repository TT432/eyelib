package io.github.tt432.eyelib.util.time;

/**
 * 与平台无关的固定步长定时器状态。
 *
 * @author TT432
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