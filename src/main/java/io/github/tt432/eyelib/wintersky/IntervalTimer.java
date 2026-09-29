package io.github.tt432.eyelib.wintersky;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 浏览器 {@code setInterval}/{@code clearInterval} 的移植接缝（emitter.js 的 playLoop 使用）。
 * 守护线程调度；句柄即 JS 的 interval id。
 */
final class IntervalTimer {

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "wintersky-tick-interval");
        t.setDaemon(true);
        return t;
    });

    private IntervalTimer() {
    }

    /** JS {@code setInterval(fn, ms)}（ms 允许小数）。 */
    static ScheduledFuture<?> setInterval(Runnable fn, double ms) {
        long nanos = (long) (ms * 1_000_000L);
        return EXECUTOR.scheduleAtFixedRate(fn, nanos, nanos, TimeUnit.NANOSECONDS);
    }

    /** JS {@code clearInterval(id)}：null/已取消均为 no-op。 */
    static void clearInterval(@Nullable ScheduledFuture<?> handle) {
        if (handle != null) {
            handle.cancel(false);
        }
    }
}
