package io.github.tt432.eyelib.client.instrument.collector;

import io.github.tt432.eyelib.client.instrument.event.InstrumentEvent;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

public final class JvmMetricCollector {
    private final MemoryMXBean memoryBean;
    private final List<GarbageCollectorMXBean> gcBeans;
    private final ThreadMXBean threadBean;
    private long previousGcCount;
    private long previousGcTimeMs;

    public JvmMetricCollector() {
        this(ManagementFactory.getMemoryMXBean(),
                ManagementFactory.getGarbageCollectorMXBeans(),
                ManagementFactory.getThreadMXBean());
    }

    // Package-private for testing with mocked MXBeans
    JvmMetricCollector(MemoryMXBean memoryBean,
                       List<GarbageCollectorMXBean> gcBeans,
                       ThreadMXBean threadBean) {
        this.memoryBean = memoryBean;
        this.gcBeans = gcBeans;
        this.threadBean = threadBean;
        // Initialize previous values
        long totalCount = 0;
        long totalTime = 0;
        for (GarbageCollectorMXBean gc : gcBeans) {
            totalCount += gc.getCollectionCount();
            totalTime += gc.getCollectionTime();
        }
        this.previousGcCount = totalCount;
        this.previousGcTimeMs = totalTime;
    }

    /**
     * Polls JVM metrics and returns InstrumentEvents.
     * Tracks deltas between successive calls for GC metrics.
     */
    public List<InstrumentEvent> poll() {
        List<InstrumentEvent> events = new ArrayList<>();

        // Heap memory
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        double heapUsedMb = heap.getUsed() / (1024.0 * 1024.0);
        double heapMaxMb = heap.getMax() / (1024.0 * 1024.0);
        events.add(new InstrumentEvent("jvm_memory", "JVM", "heap_used_mb", heapUsedMb, "MB", null));
        events.add(new InstrumentEvent("jvm_memory", "JVM", "heap_max_mb", heapMaxMb, "MB", null));

        // GC deltas
        long totalGcCount = 0;
        long totalGcTime = 0;
        for (GarbageCollectorMXBean gc : gcBeans) {
            totalGcCount += gc.getCollectionCount();
            totalGcTime += gc.getCollectionTime();
        }
        long gcCountDelta = totalGcCount - previousGcCount;
        long gcTimeDelta = totalGcTime - previousGcTimeMs;
        events.add(new InstrumentEvent("gc_activity", "JVM", "gc_collections", gcCountDelta, "count", null));
        events.add(new InstrumentEvent("gc_activity", "JVM", "gc_time_ms", gcTimeDelta, "ms", null));
        previousGcCount = totalGcCount;
        previousGcTimeMs = totalGcTime;

        // Thread counts
        events.add(new InstrumentEvent("jvm_threads", "JVM", "thread_count", threadBean.getThreadCount(), "count", null));
        events.add(new InstrumentEvent("jvm_threads", "JVM", "daemon_thread_count", threadBean.getDaemonThreadCount(), "count", null));

        return events;
    }
}
