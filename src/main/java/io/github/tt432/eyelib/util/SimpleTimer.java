package io.github.tt432.eyelib.util;

/**
 * @author TT432
 */
public class SimpleTimer {
    private long startNanoTime;
    private boolean paused;
    private long fullPausedNanoTime;
    private long pausedNanoTime;

    public SimpleTimer() {
        startNanoTime = System.nanoTime();
    }

    public void setPaused(boolean paused) {
        if (paused && !this.paused) {
            this.paused = true;
            pausedNanoTime = System.nanoTime();
        } else if (!paused && this.paused) {
            this.paused = false;
            fullPausedNanoTime += System.nanoTime() - pausedNanoTime;
        }
    }

    public long getNanoTime() {
        if (paused) return pausedNanoTime - startNanoTime - fullPausedNanoTime;
        else return System.nanoTime() - startNanoTime - fullPausedNanoTime;
    }

    public void reset() {
        startNanoTime = System.nanoTime();
        fullPausedNanoTime = 0;
    }
}
