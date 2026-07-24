package io.github.tt432.eyelib.debug.benchmark;

import com.sun.management.OperatingSystemMXBean;
import org.jspecify.annotations.Nullable;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Samples slow JVM/OS management beans away from the render thread.
 *
 * <p>Some Windows {@link OperatingSystemMXBean} queries block for roughly one
 * hundred milliseconds. Publishing one immutable snapshot per second keeps
 * that latency out of measured frame intervals.</p>
 */
final class AsyncResourceSampler implements AutoCloseable {
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final List<GarbageCollectorMXBean> garbageCollectors =
            List.copyOf(ManagementFactory.getGarbageCollectorMXBeans());
    private final @Nullable OperatingSystemMXBean operatingSystemBean;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "eyelib-benchmark-resource-sampler");
        thread.setDaemon(true);
        return thread;
    });

    private volatile Snapshot latest = Snapshot.EMPTY;
    private volatile @Nullable Throwable failure;
    private boolean started;
    private long sequence;

    AsyncResourceSampler() {
        java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
        operatingSystemBean = bean instanceof OperatingSystemMXBean osBean ? osBean : null;
    }

    void start() {
        if (started) {
            return;
        }
        started = true;
        executor.scheduleAtFixedRate(this::sample, 0L, 1L, TimeUnit.SECONDS);
    }

    Snapshot latest() {
        return latest;
    }

    @Nullable Throwable failure() {
        return failure;
    }

    private void sample() {
        try {
            MemoryUsage heap = memoryBean.getHeapMemoryUsage();
            long gcCount = 0L;
            long gcPauseMs = 0L;
            for (GarbageCollectorMXBean gc : garbageCollectors) {
                long count = gc.getCollectionCount();
                long time = gc.getCollectionTime();
                if (count > 0L) {
                    gcCount += count;
                }
                if (time > 0L) {
                    gcPauseMs += time;
                }
            }
            double processCpuLoad = operatingSystemBean == null ? -1.0 : operatingSystemBean.getProcessCpuLoad();
            latest = new Snapshot(
                    ++sequence,
                    System.nanoTime(),
                    heap.getUsed(),
                    heap.getCommitted(),
                    gcCount,
                    gcPauseMs,
                    processCpuLoad
            );
        } catch (Throwable throwable) {
            failure = throwable;
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(5L, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    record Snapshot(
            long sequence,
            long timestampNs,
            long heapUsedBytes,
            long heapCommittedBytes,
            long gcCount,
            long gcPauseMs,
            double processCpuLoad
    ) {
        private static final Snapshot EMPTY = new Snapshot(0L, 0L, 0L, 0L, 0L, 0L, -1.0);
    }
}
